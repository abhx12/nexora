package com.nexora.desktop.api

import com.nexora.desktop.repository.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

/** One JSON configuration for the whole client: unknown fields are ignored so a newer server never breaks us. */
val ApiJson = Json {
    ignoreUnknownKeys = true
    // Null fields are left out of request bodies (an absent optional field), but every other value is sent,
    // including defaults such as a chit entry's paymentMethod = CASH.
    explicitNulls = false
    encodeDefaults = true
}

/** Colour of the top-bar dot (06 section 2): green, amber while a request takes > 2 s, red when unreachable. */
enum class ServerStatus { ONLINE, SLOW, OFFLINE }

/**
 * The only class that talks HTTP. Every call returns an [ApiResult]; no exception reaches a screen.
 *
 * @param serverUrl the server address from the settings (`https://shop.example.com`), read on every call so a
 *   changed address is used immediately.
 * @param session the logged-in session; adds the bearer token and refreshes it once on a 401 (see AuthPlugin.kt).
 */
class ApiClient(
    engine: HttpClientEngine,
    private val serverUrl: () -> String,
    @PublishedApi internal val session: AuthSession,
    timeoutMillis: Long = REQUEST_TIMEOUT_MILLIS,
) {
    @PublishedApi
    internal val http = HttpClient(engine) {
        // We inspect the status ourselves and turn errors into ApiError instead of exceptions.
        expectSuccess = false
        install(ContentNegotiation) { json(ApiJson) }
        // NFR-USE-05: no request waits longer than 30 seconds.
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMillis
            connectTimeoutMillis = timeoutMillis
            socketTimeoutMillis = timeoutMillis
        }
        defaultRequest { accept(ContentType.Application.Json) }
    }

    private val _status = MutableStateFlow(ServerStatus.ONLINE)
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    /** Builds `<server>/api/v1/<path>`; actuator paths are not under `/api/v1`. */
    @PublishedApi
    internal fun url(path: String, base: String = serverUrl()): String {
        val root = base.trimEnd('/')
        return if (path.startsWith("/actuator")) root + path else "$root/api/v1$path"
    }

    suspend inline fun <reified T> get(path: String, query: Map<String, Any?> = emptyMap()): ApiResult<T> =
        send(HttpMethod.Get, path, query) { decode(it) }

    suspend inline fun <reified B, reified T> post(path: String, body: B, idempotencyKey: String? = null, authenticated: Boolean = true): ApiResult<T> =
        send(HttpMethod.Post, path, emptyMap(), idempotencyKey, authenticated, { setJson(body) }) { decode(it) }

    /** POST without a body (state actions such as `/orders/{id}/confirm`). */
    suspend inline fun <reified T> postEmpty(path: String, idempotencyKey: String? = null): ApiResult<T> =
        send(HttpMethod.Post, path, emptyMap(), idempotencyKey) { decode(it) }

    suspend inline fun <reified B, reified T> patch(path: String, body: B): ApiResult<T> =
        send(HttpMethod.Patch, path, configure = { setJson(body) }) { decode(it) }

    suspend inline fun <reified B, reified T> put(path: String, body: B): ApiResult<T> =
        send(HttpMethod.Put, path, configure = { setJson(body) }) { decode(it) }

    /** Raw bytes: CSV exports (`format=csv`) and document downloads. */
    suspend fun getBytes(path: String, query: Map<String, Any?> = emptyMap()): ApiResult<ByteArray> =
        send(HttpMethod.Get, path, query) { it.body<ByteArray>() }

    /**
     * Multipart upload (`POST /documents`). [onProgress] receives bytes sent and the total, for the progress bar.
     * The form is rebuilt on a token-refresh retry, so the file can be sent twice in that rare case.
     */
    suspend inline fun <reified T> upload(
        path: String,
        crossinline form: io.ktor.client.request.forms.FormBuilder.() -> Unit,
        noinline onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): ApiResult<T> = send(HttpMethod.Post, path, configure = {
        setBody(io.ktor.client.request.forms.MultiPartFormDataContent(io.ktor.client.request.forms.formData { form() }))
        onUpload { sent, total -> onProgress(sent, total ?: 0L) }
    }) { decode(it) }

    /** GET of an absolute address that is not stored yet (server setup tests a typed-in address). */
    suspend inline fun <reified T> getFrom(base: String, path: String): ApiResult<T> =
        send(HttpMethod.Get, path, authenticated = false, base = base) { decode(it) }

    @PublishedApi
    internal inline fun <reified B> HttpRequestBuilder.setJson(body: B) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    @PublishedApi
    internal suspend inline fun <reified T> decode(response: HttpResponse): T =
        // A 204 has no body; callers that expect nothing ask for Unit.
        if (T::class == Unit::class) Unit as T else response.body()

    /**
     * Sends one request; on a 401 lets the session refresh the token once and repeats the request once (FR-AUTH-04).
     * [read] turns a 2xx response into the value; anything else becomes an [ApiError].
     */
    @PublishedApi
    internal suspend fun <T> send(
        method: HttpMethod,
        path: String,
        query: Map<String, Any?> = emptyMap(),
        idempotencyKey: String? = null,
        authenticated: Boolean = true,
        configure: HttpRequestBuilder.() -> Unit = {},
        base: String? = null,
        read: suspend (HttpResponse) -> T,
    ): ApiResult<T> {
        suspend fun attempt(token: String?): HttpResponse = http.request(url(path, base ?: serverUrl())) {
            this.method = method
            query.forEach { (name, value) -> if (value != null) parameter(name, value.toString()) }
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
            // The same key is sent again on a retry so the server can replay instead of saving twice.
            if (idempotencyKey != null) header("Idempotency-Key", idempotencyKey)
            configure()
        }
        // Amber dot when a request takes longer than 2 seconds (06 section 2); cancelled as soon as it ends.
        return coroutineScope {
            val slow = launch {
                delay(SLOW_AFTER_MILLIS)
                if (_status.value == ServerStatus.ONLINE) _status.value = ServerStatus.SLOW
            }
            try {
                exchange(::attempt, authenticated, read)
            } finally {
                slow.cancel()
            }
        }
    }

    private suspend fun <T> exchange(
        attempt: suspend (String?) -> HttpResponse,
        authenticated: Boolean,
        read: suspend (HttpResponse) -> T,
    ): ApiResult<T> {
        return try {
            val token = if (authenticated) session.accessToken else null
            var response = attempt(token)
            if (authenticated && response.status.value == 401 && session.refreshAfter(token, this)) {
                // One retry with the new token; a second 401 is returned as is.
                response = attempt(session.accessToken)
            }
            _status.value = ServerStatus.ONLINE
            if (response.status.isSuccess()) {
                try {
                    ApiResult.Success(read(response))
                } catch (e: SerializationException) {
                    ApiResult.Failure(ApiError(response.status.value, ApiError.UNEXPECTED_RESPONSE, e.message))
                }
            } else {
                ApiResult.Failure(problem(response))
            }
        } catch (e: IOException) {
            // Connect failure, reset connection or a timeout (Ktor's timeout exceptions are IOExceptions).
            _status.value = ServerStatus.OFFLINE
            ApiResult.Failure(ApiError.network(e.message))
        }
    }

    /** Parses an error body (ProblemDetail, 05 section 1.5) into an [ApiError]. */
    private suspend fun problem(response: HttpResponse): ApiError {
        val status = response.status.value
        val parsed = try {
            ApiJson.decodeFromString<ProblemDetail>(response.bodyAsText())
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        val fallbackCode = if (status >= 500) "INTERNAL_ERROR" else ApiError.UNEXPECTED_RESPONSE
        return ApiError(
            status = status,
            code = parsed?.code ?: fallbackCode,
            detail = parsed?.detail,
            // A field can have several messages; they are shown together under the field with entered value if present
            fieldErrors = parsed?.errors.orEmpty().groupBy({ it.field }, { err ->
                if (!err.rejectedValue.isNullOrBlank()) {
                    "${err.message} (entered: \"${err.rejectedValue}\")"
                } else {
                    err.message
                }
            }).mapValues { it.value.joinToString("; ") },
        )
    }

    companion object {
        const val REQUEST_TIMEOUT_MILLIS = 30_000L

        const val SLOW_AFTER_MILLIS = 2_000L

        /** The API major version this client speaks: the 1 in /api/v1. */
        const val API_MAJOR = 1
    }
}
