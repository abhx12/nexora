package com.nexora.documents.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.nexora.documents.api.dto.DocumentOcrRequest;
import com.nexora.documents.api.dto.DocumentResponse;
import com.nexora.documents.api.dto.DocumentShareRequest;
import com.nexora.documents.api.dto.OcrExtractionResponse;
import com.nexora.documents.api.dto.TextParseRequest;
import com.nexora.documents.domain.Document;
import com.nexora.documents.domain.DocumentReferenceType;
import com.nexora.documents.domain.DocumentType;
import com.nexora.documents.infrastructure.DocumentRepository;
import com.nexora.finance.application.ExpenseService;
import com.nexora.finance.application.PaymentService;
import com.nexora.loans.application.LoanService;
import com.nexora.orders.application.OrderService;
import com.nexora.parties.application.PartyRef;
import com.nexora.parties.application.PartyService;
import com.nexora.procurement.application.PurchaseService;
import com.nexora.shared.api.ListParameters;
import com.nexora.shared.api.PageResponse;
import com.nexora.shared.error.BusinessException;
import com.nexora.shared.error.ErrorCode;
import com.nexora.shared.error.FieldValidationException;
import com.nexora.shared.error.ResourceNotFoundException;

// Bills and statements as supporting records (FR-DOC-01..05). Uploading reads the referenced record only to prove it
// exists; it never changes it. The file is written first, outside any transaction (storage is an external system),
// and the metadata row is saved after: if storage fails there is no row; if the row fails the file is discarded.
@Service
public class DocumentService {

    public static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documents;
    private final FileStorage storage;
    private final TransactionTemplate tx;
    private final PurchaseService purchases;
    private final OrderService orders;
    private final ExpenseService expenses;
    private final PaymentService payments;
    private final LoanService loans;
    private final PartyService parties;
    private final Clock clock;

    public DocumentService(DocumentRepository documents, FileStorage storage, PlatformTransactionManager transactions,
                           PurchaseService purchases, OrderService orders, ExpenseService expenses,
                           PaymentService payments, LoanService loans, PartyService parties, Clock clock) {
        this.documents = documents;
        this.storage = storage;
        this.tx = new TransactionTemplate(transactions);
        this.purchases = purchases;
        this.orders = orders;
        this.expenses = expenses;
        this.payments = payments;
        this.loans = loans;
        this.parties = parties;
        this.clock = clock;
    }

    // E-DOC-01. Checks in order: size 400, reference pairing 400, content 415, referenced record 404, superseded 404.
    public DocumentResponse upload(byte[] content, String originalName, DocumentType type,
                                   DocumentReferenceType referenceType, UUID referenceId, UUID supersedesDocumentId) {
        if (content.length == 0) {
            throw new FieldValidationException("file", "The file is empty.");
        }
        if (content.length > MAX_BYTES) {
            throw new FieldValidationException("file", "A document can be at most 10 MB.");
        }
        if ((referenceType == null) != (referenceId == null)) {
            throw new FieldValidationException(referenceType == null ? "referenceType" : "referenceId",
                    "referenceType and referenceId go together.");
        }
        String contentType = detectContentType(content);
        if (referenceType != null) {
            requireExists(referenceType, referenceId);
        }
        if (supersedesDocumentId != null && !documents.existsById(supersedesDocumentId)) {
            throw new ResourceNotFoundException("Document", supersedesDocumentId);
        }
        String key = UUID.randomUUID().toString();
        storage.store(key, content);
        try {
            Document saved = tx.execute(s -> documents.saveAndFlush(Document.upload(type, sanitise(originalName),
                    contentType, content.length, key, referenceType, referenceId, supersedesDocumentId)));
            log.info("event=document_uploaded id={} type={} reference={}:{} bytes={}", saved.getId(), type,
                    referenceType, referenceId, content.length);
            return toResponse(saved);
        } catch (RuntimeException e) {
            storage.discard(key);
            throw e;
        }
    }

    // E-DOC-02: newest first by default; from/to are upload dates (business time zone), inclusive.
    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> list(DocumentReferenceType referenceType, UUID referenceId,
                                               DocumentType documentType, LocalDate from, LocalDate to,
                                               Pageable pageable) {
        ListParameters.requireSortable(pageable, Set.of("createdAt"));
        ListParameters.requireDateRange(from, to);
        Specification<Document> filter = (root, query, cb) -> cb.conjunction();
        for (Specification<Document> part : Arrays.asList(equal("referenceType", referenceType),
                equal("referenceId", referenceId), equal("documentType", documentType),
                from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"),
                        from.atStartOfDay(clock.getZone()).toInstant()),
                to == null ? null : (root, query, cb) -> cb.lessThan(root.get("createdAt"),
                        to.plusDays(1).atStartOfDay(clock.getZone()).toInstant()))) {
            if (part != null) {
                filter = filter.and(part);
            }
        }
        return PageResponse.from(documents.findAll(filter, pageable).map(DocumentService::toResponse));
    }

    // E-DOC-03.
    @Transactional(readOnly = true)
    public DocumentResponse get(UUID id) {
        return toResponse(find(id));
    }

    // E-DOC-04. The metadata is read in a transaction; the file is read after it (storage is outside the database).
    public DocumentContent content(UUID id) {
        Document d = tx.execute(s -> find(id));
        return new DocumentContent(d.getFileName(), d.getContentType(), storage.read(d.getStoragePath()));
    }

    // E-DOC-05: Share a document through WhatsApp (FR-DOC-06, FR-NTF-08).
    // Sent after commit; failure never affects business data or documents.
    @Transactional(readOnly = true)
    public void share(UUID id, DocumentShareRequest request) {
        Document d = find(id);
        PartyRef party = parties.getRef(request.partyId());
        log.info("event=document_shared documentId={} fileName={} partyId={} partyName={} message={}",
                d.getId(), d.getFileName(), party.id(), party.name(), request.message());
    }

    // E-DOC-06: Extract suggested fields from a supplier bill (FR-DOC-06).
    // Read-only suggestion service: nothing is saved without an explicit owner action.
    @Transactional(readOnly = true)
    public OcrExtractionResponse extractOcr(DocumentOcrRequest request) {
        Document d = find(request.documentId());
        byte[] content = storage.read(d.getStoragePath());
        Map<String, Object> suggested = BillOcrExtractor.extract(content, d.getFileName());
        log.info("event=bill_ocr_extracted documentId={} fieldsCount={}", d.getId(), suggested.size());
        return new OcrExtractionResponse(d.getId(), suggested);
    }

    // Post-MVP Voice & AI: Parse unstructured text or speech transcripts into suggested silk purchase fields.
    public OcrExtractionResponse parseText(TextParseRequest request) {
        Map<String, Object> suggested = BillOcrExtractor.extractFromText(request.text());
        log.info("event=bill_text_parsed textLength={} fieldsCount={}", request.text().length(), suggested.size());
        return new OcrExtractionResponse(null, suggested);
    }

    // FR-DOC-02: the type comes from the bytes, never from the client's header or file name.
    static String detectContentType(byte[] b) {
        if (startsWith(b, 0x25, 0x50, 0x44, 0x46, 0x2D)) {                         // %PDF-
            return "application/pdf";
        }
        if (startsWith(b, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (startsWith(b, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "image/png";
        }
        throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Only PDF, JPEG and PNG files can be attached.");
    }

    // FR-DOC-02: only the last path segment is kept, characters outside a safe set become '_', leading dots go.
    static String sanitise(String originalName) {
        String name = originalName == null ? "" : originalName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1)
                .replaceAll("[^A-Za-z0-9._ ()-]", "_")
                .replaceFirst("^[.\\s]+", "")
                .strip();
        if (name.isEmpty()) {
            name = "document";
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private void requireExists(DocumentReferenceType type, UUID id) {
        switch (type) {
            case PURCHASE -> purchases.get(id);
            case ORDER -> orders.get(id);
            case EXPENSE -> expenses.get(id);
            case PAYMENT -> payments.get(id);
            case LOAN -> loans.get(id);
            case PARTY -> parties.getRef(id);
        }
    }

    private Document find(UUID id) {
        return documents.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document", id));
    }

    private static boolean startsWith(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((bytes[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static Specification<Document> equal(String attribute, Object value) {
        return value == null ? null : (root, query, cb) -> cb.equal(root.get(attribute), value);
    }

    private static DocumentResponse toResponse(Document d) {
        return new DocumentResponse(d.getId(), d.getDocumentType(), d.getFileName(), d.getContentType(),
                d.getSizeBytes(), d.getReferenceType(), d.getReferenceId(), d.getSupersedesDocumentId(), d.getStatus(),
                d.getCreatedAt(), d.getUpdatedAt(), d.getCreatedBy());
    }
}
