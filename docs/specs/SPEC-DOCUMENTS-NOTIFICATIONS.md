# OpenSpec: Document Sharing, OCR & Notification Management (FR-DOC-06, FR-NTF-04, FR-NTF-07, FR-NTF-08, E-DOC-05, E-DOC-06, E-NTF-04)

- **Feature ID:** FEAT-DOCUMENTS-AND-NOTIFICATIONS
- **Requirements Covered:** FR-DOC-06, FR-NTF-04, FR-NTF-07, FR-NTF-08, E-DOC-05, E-DOC-06, E-NTF-04, NFR-REL-05, NFR-SEC-09, NFR-USE-01
- **Milestone:** M19
- **Status:** APPROVED (Spec-Driven Development)
- **Author:** Antigravity Pair Programmer
- **Date:** 2026-10-23

---

## 1. Executive Summary & Business Motivation

While the core platform supports document uploads and basic scheduled notifications, day-to-day operations in a silk yarn trading business require seamless communication with parties, fast data entry, and robust fault-tolerance:
1. **Document Sharing via WhatsApp (FR-DOC-06, FR-NTF-08, E-DOC-05):** Allowing the owner to share customer bills, statements, or receipts directly to customer/supplier WhatsApp with an optional message. Sending occurs strictly after transaction commit, and any external provider failure never affects business data.
2. **Bill Data Extraction / OCR (FR-DOC-06, E-DOC-06):** Extracting key purchase bill details (weight, rate per kg, gross amounts, supplier references) from uploaded supplier invoices to pre-populate purchase creation workflows without manual re-keying, with no automatic data modification without explicit owner confirmation.
3. **Notification Retry Lifecycle (FR-NTF-04, FR-NTF-07, FR-NTF-08, E-NTF-04):** Providing explicit manual retry for `FAILED` notifications (transitions `FAILED -> PENDING`), resetting attempts and error state so the notification dispatcher can re-attempt delivery.

---

## 2. API Specifications & Contracts

### 2.1 Retry Notification (`POST /api/v1/notifications/{id}/retry`)
- **Permission:** `FINANCE_MANAGE`
- **Path Parameter:** `id` (`UUID`, required)
- **Request Body:** None
- **Preconditions:**
  - Notification must exist (404 `RESOURCE_NOT_FOUND`).
  - Notification status must be `FAILED` (409 `INVALID_STATE_TRANSITION` if status is not `FAILED`).
- **Behavior:**
  - Resets `status = NotificationStatus.PENDING`
  - Resets `attempts = 0`
  - Clears `lastError = null`
- **Output:** `200 OK` with `NotificationResponse`.

### 2.2 Share Document (`POST /api/v1/documents/{id}/share`)
- **Permission:** `DOCUMENT_MANAGE`
- **Path Parameter:** `id` (`UUID`, required)
- **Request Body:**
  ```json
  {
    "partyId": "uuid",
    "message": "Optional message (max 500 characters)"
  }
  ```
- **Validation:**
  - `partyId` (`@NotNull`, UUID)
  - `message` (`@Size(max = 500)`)
  - Document `{id}` must exist (404 `RESOURCE_NOT_FOUND`).
  - Party `{partyId}` must exist (404 `RESOURCE_NOT_FOUND`).
- **Behavior:**
  - Dispatches share request after commit.
  - Failure of the delivery channel never causes database rollback.
- **Output:** `202 Accepted` (empty body).

### 2.3 Extract Data from Supplier Bill / OCR (`POST /api/v1/documents/ocr-extractions`)
- **Permission:** `DOCUMENT_MANAGE`
- **Request Body:**
  ```json
  {
    "documentId": "uuid"
  }
  ```
- **Validation:**
  - `documentId` (`@NotNull`, UUID)
  - Document must exist (404 `RESOURCE_NOT_FOUND`).
- **Behavior:**
  - Inspects document content / text streams.
  - Heuristically extracts suggested fields: `orderedWeightKg`, `ratePerKg`, `grossAmount`, `supplierName`, `billNumber`.
  - Does NOT alter any database business record.
- **Output:** `200 OK` with:
  ```json
  {
    "documentId": "uuid",
    "suggested": {
      "orderedWeightKg": 250.000,
      "ratePerKg": 800.00,
      "grossAmount": 200000.00
    }
  }
  ```

---

## 3. Desktop Client Integration
- `NotificationRepository.retry(id)`
- `DocumentRepository.share(id, partyId, message)`
- `DocumentRepository.extractOcr(documentId)`
- Update `NotificationListViewModel` to support retrying failed notifications.
