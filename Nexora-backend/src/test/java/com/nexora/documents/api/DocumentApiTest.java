package com.nexora.documents.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.nexora.support.AbstractApiTest;
import com.nexora.support.AuthTokens;

class DocumentApiTest extends AbstractApiTest {

    private static final byte[] PDF = "%PDF-1.7\n1 0 obj\n<<>>\nendobj\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};

    @Test
    @DisplayName("FR-DOC-01 FR-DOC-02 upload stores under generated id")
    void upload_storesUnderGeneratedId() throws Exception {
        String purchase = createPurchase(data.supplier().id(), "RAW_SILK", "100.000", "800.00");
        // AC1: a PDF bill for the purchase -> 201 with id, name, size, type and the reference
        String id = body(upload(file("bill.pdf", PDF), "PURCHASE_BILL", "PURCHASE", purchase)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.startsWith("/api/v1/documents/")))
                .andExpect(jsonPath("$.fileName").value("bill.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.sizeBytes").value(PDF.length))
                .andExpect(jsonPath("$.referenceType").value("PURCHASE"))
                .andExpect(jsonPath("$.referenceId").value(purchase))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.storagePath").doesNotExist())).get("id").asString();
        // stored inside the storage root under a generated name (not the client's), with the same bytes
        String key = jdbc.queryForObject("select storage_path from documents where id = ?::uuid", String.class, id);
        assertThat(UUID.fromString(key)).isNotNull();
        assertThat(Files.readAllBytes(DOCUMENTS_DIR.resolve(key))).isEqualTo(PDF);

        // AC3: an .exe is 415; AC4: an unknown reference is 404; AC5: STAFF is 403; referenceId alone is 400
        upload(file("setup.exe", "MZ\u0090\u0000binary".getBytes(StandardCharsets.ISO_8859_1)), "OTHER", null, null)
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        upload(file("bill.pdf", PDF), "PURCHASE_BILL", "PURCHASE", UUID.randomUUID().toString())
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        upload(file("bill.pdf", PDF), "PURCHASE_BILL", null, purchase).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(request(file("bill.pdf", PDF), "PURCHASE_BILL", null, null)
                .header(HttpHeaders.AUTHORIZATION, AuthTokens.bearer(tokens.staff()))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        // a JSON body instead of a multipart form -> 400 MALFORMED_REQUEST
        asOwner(postJson("/api/v1/documents", "{\"documentType\":\"OTHER\"}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        // FR-DOC-02 AC2: named and declared as PDF but not a PDF inside -> 415; nothing stored for any refusal
        upload(new MockMultipartFile("file", "bill.pdf", "application/pdf", "<html>".getBytes(StandardCharsets.UTF_8)),
                "PURCHASE_BILL", null, null).andExpect(status().isUnsupportedMediaType());
        assertThat(jdbc.queryForObject("select count(*) from documents", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("FR-DOC-02 upload path traversal name is sanitised")
    void upload_pathTraversalName_isSanitised() throws Exception {
        // AC1: the client name tries to climb out of the storage root
        String id = body(upload(file("../../etc/passwd.pdf", PDF), "OTHER", null, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("passwd.pdf"))).get("id").asString();
        String key = jdbc.queryForObject("select storage_path from documents where id = ?::uuid", String.class, id);
        Path stored = DOCUMENTS_DIR.resolve(key).normalize();
        assertThat(stored.getParent()).isEqualTo(DOCUMENTS_DIR.normalize());
        assertThat(Files.exists(stored)).isTrue();
        // odd characters and Windows separators are neutralised too
        upload(file("C:\\bills\\..\\inv<1>;rm -rf.png", PNG), "OTHER", null, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("inv_1__rm -rf.png"));
    }

    @Test
    @DisplayName("NFR-SEC-03 upload over10 mb is rejected")
    void upload_over10Mb_isRejected() throws Exception {
        // AC2 of FR-DOC-01: 11 MB -> 400 VALIDATION_FAILED, nothing stored
        byte[] big = Arrays.copyOf(PDF, 11 * 1024 * 1024);
        upload(file("big.pdf", big), "OTHER", null, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("file"));
        // exactly 10 MB is accepted
        upload(file("max.pdf", Arrays.copyOf(PDF, 10 * 1024 * 1024)), "OTHER", null, null)
                .andExpect(status().isCreated());
        // an empty file is 400 as well
        upload(file("empty.pdf", new byte[0]), "OTHER", null, null).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from documents", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("FR-DOC-03 list and download content")
    void list_andDownloadContent() throws Exception {
        String purchase = createPurchase(data.supplier().id(), "RAW_SILK", "100.000", "800.00");
        String bill = id(upload(file("bill.pdf", PDF), "PURCHASE_BILL", "PURCHASE", purchase));
        id(upload(file("weighbridge.png", PNG), "OTHER", "PURCHASE", purchase));
        id(upload(file("statement.pdf", PDF), "BANK_STATEMENT", null, null));

        // AC1: both documents of the purchase, newest first; other filters
        asOwner(get("/api/v1/documents?referenceType=PURCHASE&referenceId=" + purchase)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].fileName").value("weighbridge.png"))
                .andExpect(jsonPath("$.content[1].id").value(bill));
        asOwner(get("/api/v1/documents?documentType=BANK_STATEMENT")).andExpect(jsonPath("$.totalElements").value(1));
        asOwner(get("/api/v1/documents?from=" + today() + "&to=" + today())).andExpect(jsonPath("$.totalElements").value(3));
        asOwner(get("/api/v1/documents/" + bill)).andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("PURCHASE_BILL"));
        // download: the bytes, the stored type, a safe file name and the length
        byte[] downloaded = asOwner(get("/api/v1/documents/" + bill + "/content")).andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bill.pdf\""))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, PDF.length))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(downloaded).isEqualTo(PDF);

        // AC2: unknown id 404; AC3: no token 401
        asOwner(get("/api/v1/documents/" + UUID.randomUUID() + "/content")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/documents/" + bill + "/content")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("FR-DOC-04 overwrite or delete is not supported")
    void overwriteOrDelete_isNotSupported() throws Exception {
        String id = id(upload(file("bill.pdf", PDF), "PURCHASE_BILL", null, null));
        // AC2: PUT and DELETE are 405
        asOwner(put("/api/v1/documents/" + id).contentType("application/json").content("{}"))
                .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        asOwner(delete("/api/v1/documents/" + id)).andExpect(status().isMethodNotAllowed());
        asOwner(get("/api/v1/documents/" + id)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("FR-DOC-04 upload superseding document keeps both listed")
    void upload_supersedingDocument_keepsBothListed() throws Exception {
        String purchase = createPurchase(data.supplier().id(), "RAW_SILK", "100.000", "800.00");
        String first = id(upload(file("bill.pdf", PDF), "PURCHASE_BILL", "PURCHASE", purchase));
        // AC1: a corrected bill supersedes the first; both stay listed, the first unchanged
        String second = body(owner(request(file("bill-corrected.pdf", PDF), "PURCHASE_BILL", "PURCHASE", purchase)
                .param("supersedesDocumentId", first)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.supersedesDocumentId").value(first))).get("id").asString();
        asOwner(get("/api/v1/documents?referenceType=PURCHASE&referenceId=" + purchase))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(second))
                .andExpect(jsonPath("$.content[1].id").value(first))
                .andExpect(jsonPath("$.content[1].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[1].supersedesDocumentId").doesNotExist());
    }

    @Test
    @DisplayName("FR-DOC-04 supersedes unknown document returns404")
    void supersedesUnknownDocument_returns404() throws Exception {
        long filesBefore = storedFiles();
        owner(request(file("bill.pdf", PDF), "PURCHASE_BILL", null, null)
                .param("supersedesDocumentId", UUID.randomUUID().toString())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        // neither a row nor a file is left behind
        assertThat(jdbc.queryForObject("select count(*) from documents", Integer.class)).isZero();
        assertThat(storedFiles()).isEqualTo(filesBefore);
    }

    @Test
    @DisplayName("FR-DOC-05 upload does not change referenced record")
    void upload_doesNotChangeReferencedRecord() throws Exception {
        String purchase = createPurchase(data.supplier().id(), "RAW_SILK", "100.000", "800.00");
        receive(purchase, "50.000", "50.000", "0.000").andExpect(status().isCreated());
        String before = asOwner(get("/api/v1/purchases/" + purchase)).andReturn().getResponse().getContentAsString();

        // AC1 + AC2: a bill with an extra "amount" form field changes nothing on the purchase
        owner(request(file("bill.pdf", PDF), "PURCHASE_BILL", "PURCHASE", purchase).param("amount", "1.00")
                .param("status", "CANCELLED")).andExpect(status().isCreated());
        asOwner(get("/api/v1/purchases/" + purchase)).andExpect(status().isOk())
                .andExpect(content().json(before, org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("E-DOC-05 share document via WhatsApp returns 202")
    void share_documentViaWhatsApp_returns202() throws Exception {
        String id = id(upload(file("bill.pdf", PDF), "SALES_BILL", null, null));
        String customerId = data.customer().id().toString();

        // AC1: valid share request returns 202 Accepted
        asOwner(postJson("/api/v1/documents/" + id + "/share",
                """
                {"partyId":"%s","message":"Here is your sales bill."}
                """.formatted(customerId)))
                .andExpect(status().isAccepted());

        // AC2: unknown party returns 404
        asOwner(postJson("/api/v1/documents/" + id + "/share",
                """
                {"partyId":"%s","message":"Hello"}
                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // AC3: unknown document returns 404
        asOwner(postJson("/api/v1/documents/" + UUID.randomUUID() + "/share",
                """
                {"partyId":"%s","message":"Hello"}
                """.formatted(customerId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // AC4: message exceeding 500 chars returns 400 VALIDATION_FAILED
        asOwner(postJson("/api/v1/documents/" + id + "/share",
                """
                {"partyId":"%s","message":"%s"}
                """.formatted(customerId, "x".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        // AC5: STAFF without DOCUMENT_MANAGE returns 403
        mockMvc.perform(postJson("/api/v1/documents/" + id + "/share",
                """
                {"partyId":"%s","message":"Hello"}
                """.formatted(customerId))
                .header(HttpHeaders.AUTHORIZATION, AuthTokens.bearer(tokens.staff())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("E-DOC-06 extract data from bill document via OCR")
    void ocr_supplierBill_extractsSuggestedFields() throws Exception {
        byte[] billPdf = ("%PDF-1.4\n1 0 obj\n<< /Length 200 >>\nstream\n"
                + "BT /F1 12 Tf (Bill No: INV-2026-09) Tj 0 -20 Td (Supplier: Silk Exporters) Tj 0 -20 Td "
                + "(Weight: 250.000 kg) Tj 0 -20 Td (Rate: 800.00 / kg) Tj ET\nendstream\nendobj\n%%EOF\n")
                .getBytes(StandardCharsets.US_ASCII);

        String id = id(upload(file("supplier_bill.pdf", billPdf), "PURCHASE_BILL", null, null));

        // AC1: extracts suggested fields
        asOwner(postJson("/api/v1/documents/ocr-extractions", "{\"documentId\":\"" + id + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(id))
                .andExpect(jsonPath("$.suggested.orderedWeightKg").value(250.000))
                .andExpect(jsonPath("$.suggested.ratePerKg").value(800.00))
                .andExpect(jsonPath("$.suggested.grossAmount").value(200000.00))
                .andExpect(jsonPath("$.suggested.billNumber").value("INV-2026-09"))
                .andExpect(jsonPath("$.suggested.supplierName").value("Silk Exporters"));

        // AC2: unknown document returns 404
        asOwner(postJson("/api/v1/documents/ocr-extractions", "{\"documentId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // AC3: STAFF without DOCUMENT_MANAGE returns 403
        mockMvc.perform(postJson("/api/v1/documents/ocr-extractions", "{\"documentId\":\"" + id + "\"}")
                .header(HttpHeaders.AUTHORIZATION, AuthTokens.bearer(tokens.staff())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void parseText_tamilTranscript_extractsSuggestedFields() throws Exception {
        String payload = "{\"text\":\"ஸ்ரீ பாலாஜி சில்க்ஸ் 25.500 கிலோ ரேட் 4500 பில் எண் 88 கடன் 15 நாட்கள்\"}";

        asOwner(postJson("/api/v1/documents/parse-text", payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggested.supplierName").value("ஸ்ரீ பாலாஜி சில்க்ஸ்"))
                .andExpect(jsonPath("$.suggested.orderedWeightKg").value(25.500))
                .andExpect(jsonPath("$.suggested.ratePerKg").value(4500.00))
                .andExpect(jsonPath("$.suggested.billNumber").value("88"))
                .andExpect(jsonPath("$.suggested.paymentType").value("CREDIT"))
                .andExpect(jsonPath("$.suggested.creditDays").value(15))
                .andExpect(jsonPath("$.suggested.grossAmount").value(114750.00));
    }

    private static long storedFiles() throws Exception {
        try (var files = Files.list(DOCUMENTS_DIR)) {
            return files.count();
        }
    }

    private ResultActions upload(MockMultipartFile file, String type, String referenceType, String referenceId)
            throws Exception {
        return owner(request(file, type, referenceType, referenceId));
    }

    // A multipart request sent as the owner (the multipart builder is not the type asOwner takes).
    private ResultActions owner(MockMultipartHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, AuthTokens.bearer(tokens.owner())));
    }

    private static MockMultipartHttpServletRequestBuilder request(MockMultipartFile file, String type,
                                                                  String referenceType, String referenceId) {
        MockMultipartHttpServletRequestBuilder b = multipart("/api/v1/documents").file(file);
        b.param("documentType", type);
        if (referenceType != null) {
            b.param("referenceType", referenceType);
        }
        if (referenceId != null) {
            b.param("referenceId", referenceId);
        }
        return b;
    }

    private static MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile("file", name, "application/octet-stream", bytes);
    }

    private String id(ResultActions result) throws Exception {
        return body(result.andExpect(status().isCreated())).get("id").asString();
    }
}
