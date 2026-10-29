package com.nexora.desktop.repository

import com.nexora.desktop.api.ApiClient
import com.nexora.desktop.api.dto.DocumentResponse
import com.nexora.desktop.api.dto.PageResponse
import com.nexora.desktop.viewmodel.common.ListQuery
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import java.nio.file.Files
import java.nio.file.Path

/** Supporting documents (05 section 18): list, upload (multipart), download. */
class DocumentRepository(private val api: ApiClient) {

    suspend fun list(query: ListQuery): ApiResult<PageResponse<DocumentResponse>> = api.get("/documents", query.params())

    suspend fun content(id: String): ApiResult<ByteArray> = api.getBytes("/documents/$id/content")

    suspend fun upload(
        file: Path,
        contentType: String,
        documentType: String,
        referenceType: String?,
        referenceId: String?,
        onProgress: (Long, Long) -> Unit,
    ): ApiResult<DocumentResponse> {
        val bytes = Files.readAllBytes(file)
        return api.upload("/documents", {
            append(
                "file",
                bytes,
                Headers.build {
                    append(HttpHeaders.ContentType, contentType)
                    append(HttpHeaders.ContentDisposition, "filename=\"${file.fileName}\"")
                },
            )
            append("documentType", documentType)
            if (referenceType != null && referenceId != null) {
                append("referenceType", referenceType)
                append("referenceId", referenceId)
            }
        }, onProgress)
    }

    suspend fun share(id: String, partyId: String, message: String? = null): ApiResult<Unit> =
        api.post("/documents/$id/share", com.nexora.desktop.api.dto.DocumentShareRequest(partyId, message))

    suspend fun extractOcr(documentId: String): ApiResult<com.nexora.desktop.api.dto.OcrExtractionResponse> =
        api.post("/documents/ocr-extractions", com.nexora.desktop.api.dto.DocumentOcrRequest(documentId))

    suspend fun parseText(text: String): ApiResult<com.nexora.desktop.api.dto.OcrExtractionResponse> =
        api.post("/documents/parse-text", com.nexora.desktop.api.dto.TextParseRequest(text))
}
