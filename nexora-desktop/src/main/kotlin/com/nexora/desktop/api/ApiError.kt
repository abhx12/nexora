package com.nexora.desktop.api

import kotlinx.serialization.Serializable

/**
 * What went wrong with a request, in a form the screens can use directly.
 * [code] is the stable server code (`INSUFFICIENT_INVENTORY`, ...) or one of the client codes below;
 * [fieldErrors] maps a request field name to the message shown under that field.
 */
data class ApiError(
    val status: Int,
    val code: String,
    val detail: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
) {
    /** True when the server never answered (connect failure, reset, timeout). */
    val isNetwork: Boolean get() = code == NETWORK

    /**
     * A retry of the same submit may succeed: the server was unreachable or failed internally.
     * A 4xx answer is final for this input, so it is not retryable (06 section 3.3).
     */
    val isRetryable: Boolean get() = isNetwork || status >= 500

    companion object {
        /** Client-side code: the server could not be reached or did not answer within 30 seconds. */
        const val NETWORK = "NETWORK"

        /** Client-side code: the server answered with something that is not the documented shape. */
        const val UNEXPECTED_RESPONSE = "UNEXPECTED_RESPONSE"

        fun network(detail: String?) = ApiError(status = 0, code = NETWORK, detail = detail)
    }
}

/** RFC 9457 ProblemDetail as sent by the API (05 section 1.5); unknown fields are ignored. */
@Serializable
internal data class ProblemDetail(
    val status: Int? = null,
    val code: String? = null,
    val detail: String? = null,
    val errors: List<FieldErrorDto> = emptyList(),
)

@Serializable
internal data class FieldErrorDto(
    val field: String,
    val message: String,
    val rejectedValue: String? = null,
)
