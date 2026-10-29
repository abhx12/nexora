package com.nexora.documents.api;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nexora.documents.api.dto.DocumentOcrRequest;
import com.nexora.documents.api.dto.DocumentResponse;
import com.nexora.documents.api.dto.DocumentShareRequest;
import com.nexora.documents.api.dto.OcrExtractionResponse;
import com.nexora.documents.api.dto.TextParseRequest;
import com.nexora.documents.application.DocumentContent;
import com.nexora.documents.application.DocumentService;
import com.nexora.documents.domain.DocumentReferenceType;
import com.nexora.documents.domain.DocumentType;
import com.nexora.shared.api.PageResponse;

import jakarta.validation.Valid;

// Bills and statements (E-DOC-01..04). No PUT and no DELETE: documents are never overwritten or removed (FR-DOC-04).
@RestController
@RequestMapping("/api/v1/documents")
class DocumentController {

    private final DocumentService documents;

    DocumentController(DocumentService documents) {
        this.documents = documents;
    }

    // Any other form field (an "amount", say) is simply not read (FR-DOC-05 AC2). No "consumes" restriction: the
    // permission is checked before the body (a non-multipart request is then 400 MALFORMED_REQUEST).
    @PostMapping
    @PreAuthorize("hasAuthority('DOCUMENT_MANAGE')")
    ResponseEntity<DocumentResponse> upload(@RequestParam("file") MultipartFile file,
                                            @RequestParam DocumentType documentType,
                                            @RequestParam(required = false) DocumentReferenceType referenceType,
                                            @RequestParam(required = false) UUID referenceId,
                                            @RequestParam(required = false) UUID supersedesDocumentId)
            throws IOException {
        DocumentResponse created = documents.upload(file.getBytes(), file.getOriginalFilename(), documentType,
                referenceType, referenceId, supersedesDocumentId);
        return ResponseEntity.created(URI.create("/api/v1/documents/" + created.id())).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_MANAGE')")
    PageResponse<DocumentResponse> list(
            @RequestParam(required = false) DocumentReferenceType referenceType,
            @RequestParam(required = false) UUID referenceId,
            @RequestParam(required = false) DocumentType documentType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return documents.list(referenceType, referenceId, documentType, from, to, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_MANAGE')")
    DocumentResponse get(@PathVariable UUID id) {
        return documents.get(id);
    }

    @GetMapping("/{id}/content")
    @PreAuthorize("hasAuthority('DOCUMENT_MANAGE')")
    ResponseEntity<byte[]> content(@PathVariable UUID id) {
        DocumentContent c = documents.content(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(c.contentType()))
                .contentLength(c.bytes().length)
                .header("Content-Disposition", ContentDisposition.attachment().filename(c.fileName()).build().toString())
                .body(c.bytes());
    }

    // E-DOC-05.
    @PostMapping("/{id}/share")
    @PreAuthorize("hasAuthority('DOCUMENT_MANAGE')")
    ResponseEntity<Void> share(@PathVariable UUID id, @Valid @RequestBody DocumentShareRequest request) {
        documents.share(id, request);
        return ResponseEntity.accepted().build();
    }

    // E-DOC-06.
    @PostMapping("/ocr-extractions")
    @PreAuthorize("hasAuthority('DOCUMENT_MANAGE')")
    OcrExtractionResponse ocrExtractions(@Valid @RequestBody DocumentOcrRequest request) {
        return documents.extractOcr(request);
    }

    // Post-MVP Voice & AI: Parse unstructured text into suggested silk purchase fields
    @PostMapping("/parse-text")
    @PreAuthorize("hasAuthority('PURCHASE_CREATE')")
    OcrExtractionResponse parseText(@Valid @RequestBody TextParseRequest request) {
        return documents.parseText(request);
    }
}
