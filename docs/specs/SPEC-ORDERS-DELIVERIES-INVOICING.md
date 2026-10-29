# OpenSpec: Orders, Partial Deliveries & Sales Bill Invoicing (FR-ORD-19, FR-DOC-06, E-ORD-10, E-ORD-12)

- **Feature ID:** FEAT-ORDERS-DELIVERIES-INVOICING
- **Requirements Covered:** FR-ORD-19, FR-DOC-06, NFR-DATA-01, NFR-SEC-09, NFR-USE-01, NFR-REL-02
- **Milestone:** M14
- **Status:** APPROVED (Spec-Driven Development)
- **Author:** Antigravity Pair Programmer
- **Date:** 2026-10-23

---

## 1. Executive Summary & Business Motivation

Silk yarn orders frequently involve multi-item requests or partial delivery schedules. Customers may collect warp or raw silk in batches as items become ready. Furthermore, upon delivery or completion, the customer requires an official Sales Bill / Tax Invoice document for their records and GST filing.

This specification implements:
1. **Per-Item & Partial Delivery (E-ORD-10, FR-ORD-19):**
   - `POST /api/v1/orders/{id}/items/{itemId}/deliveries` (`ORDER_MANAGE`, `@Idempotent`)
   - Allows delivering a specific item or partial weight for an item.
   - Records material movements (`DELIVERY` from `FINISHED_STOCK` or `DIRECT_SALE` from `RAW_STOCK` to `CUSTOMER`).
   - Ensures delivered weight does not exceed the line item's required weight or available stock.
   - Updates item status to `DELIVERED` when fully delivered, and updates order to `DELIVERED` when all items are delivered.
2. **Sales Bill PDF Generation (E-ORD-12, FR-DOC-06):**
   - `POST /api/v1/orders/{id}/bill` (`ORDER_MANAGE`)
   - Generates a clean, valid PDF-1.4 Sales Bill containing order details, customer info, itemized rates/amounts, and totals.
   - Attaches the PDF as a `SALES_BILL` document associated with the `ORDER`.
   - Supports superseding previous versions if regenerated after corrections.
3. **Desktop Client Integration:**
   - Adds per-item delivery option and "Generate Sales Bill" action to the order detail screen.
   - Enables viewing and downloading generated sales bills.

---

## 2. API Contract

### 2.1 E-ORD-10 `POST /api/v1/orders/{id}/items/{itemId}/deliveries`
- **Permission:** `ORDER_MANAGE`
- **Header:** `Idempotency-Key` (required)
- **Request Body:**
```json
{
  "deliveryDate": "2026-10-23",
  "quantityKg": 25.000,
  "notes": "partial delivery 1"
}
```
- **Validation:**
  - `deliveryDate`: `@NotNull`, `@BusinessDate(allowFuture = false)`
  - `quantityKg`: `@NotNull`, `@DecimalMin("0.001")`, at most 3 decimal places
  - Order must not be `PLACED` or `CANCELLED` (409 `INVALID_STATE_TRANSITION`).
  - Item must belong to order (404 `RESOURCE_NOT_FOUND`).
  - Remaining weight check: Already delivered + `quantityKg` cannot exceed `item.requiredWeightKg` (409 `INVALID_STATE_TRANSITION`).
  - Stock check: Inventory must have sufficient stock (409 `INSUFFICIENT_INVENTORY`).
- **Response:** `200 OK` with `OrderResponse`.

### 2.2 E-ORD-12 `POST /api/v1/orders/{id}/bill`
- **Permission:** `ORDER_MANAGE`
- **Request Body:** None (empty)
- **Validation:**
  - Order must exist (404 `RESOURCE_NOT_FOUND`).
  - Order must be `DELIVERED` or `COMPLETED` (409 `INVALID_STATE_TRANSITION`).
- **Response:** `201 Created` with `Location: /api/v1/documents/{documentId}` and `DocumentResponse`.
