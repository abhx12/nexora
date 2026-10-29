# OpenSpec: Customer Returns & Damaged Goods (FR-ORD-20 & FR-INV-17)

- **Feature ID:** FEAT-ORD-RETURNS
- **Requirements Covered:** FR-ORD-20, FR-INV-17, NFR-DATA-01, NFR-DATA-02, NFR-DATA-03, NFR-DATA-05, NFR-DATA-06, NFR-SEC-09, NFR-REL-02
- **Milestone:** M10
- **Status:** APPROVED (Spec-Driven Development)
- **Author:** Antigravity Pair Programmer
- **Date:** 2026-10-18

---

## 1. Executive Summary & Business Motivation

In the silk-yarn trading and warping business, customers occasionally return material after delivery due to quality defects (e.g., uneven tension, reed marks, weaving edge defects, discoloration) or transit damage.

Currently, SmartSilk only supports forward delivery (`DELIVERY` and `DIRECT_SALE`). When a customer returns goods:
1. Usable goods must re-enter inventory (`FINISHED_STOCK` for warp, `RAW_STOCK` for raw silk or vuda warp) via a `CUSTOMER_RETURN` movement.
2. Defective/unusable goods must be recorded directly into `WASTAGE` via a `CUSTOMER_RETURN` movement so they are never conflated with sellable stock.
3. Every return must be recorded in an immutable, append-only `customer_returns` ledger with a unique business number (`CRN-YYYY-NNNN`).
4. Financial obligations and Customer Outstanding must be credited by `returned_weight_kg * rate_per_kg`, ensuring customers are never over-billed for returned goods.
5. Strict invariants prevent returning more than was delivered or returning against undelivered orders.

---

## 2. Business Invariants & Domain Rules

1. **Eligible Order Statuses:** An order must be in `DELIVERED` or `COMPLETED` status. Returns against `PLACED`, `CONFIRMED`, `IN_PROGRESS`, `READY`, or `CANCELLED` orders are strictly rejected with 409 `INVALID_STATE_TRANSITION`.
2. **Item-Level Tracking:** A return must specify an `order_item_id` belonging to the order. If the item belongs to another order or is unknown: 404 `RESOURCE_NOT_FOUND`.
3. **Weight Boundary Invariant:** For any order item:
   $$\sum \text{returned\_weight\_kg} \le \text{item.required\_weight\_kg}$$
   Attempting to return more than the remaining delivered quantity is rejected with 409 `INSUFFICIENT_INVENTORY`.
4. **Usable vs Damaged Routing:**
   - When `usable = true`:
     - Warp items (`SILK_WARP`, `KORA_WARP`) move `CUSTOMER` $\to$ `FINISHED_STOCK`.
     - Trading items (`RAW_SILK`, `VUDA_WARP`) move `CUSTOMER` $\to$ `RAW_STOCK`.
     - Stock projection in `inventory_balances` increases accordingly.
   - When `usable = false`:
     - Movement moves `CUSTOMER` $\to$ `WASTAGE`.
     - `inventory_balances` is unaffected (neither `CUSTOMER` nor `WASTAGE` is an internal balance location).
5. **Ledger Immutability (ADR-032):**
   - The `customer_returns` table is append-only.
   - Rows can never be updated, deleted, or truncated. Database triggers (`prevent_row_modification`) and role privilege revocations enforce this at the engine level.
6. **Financial Obligation Credit:**
   - Outstanding on the order item is reduced by $\text{returned\_weight\_kg} \times \text{rate\_per\_kg}$ (rounded `HALF_UP` to scale 2).
   - If an order was already paid in full, the credit increases the customer's unallocated payment / advance balance.
7. **Pessimistic Lock Ordering (ADR-028):**
   To prevent concurrent return race conditions and deadlocks:
   1. `customer_orders` row (`FOR UPDATE`)
   2. `inventory_balances` rows in ascending `(product_type, location)` order
   3. `CRN` sequence row (`FOR UPDATE`)
   4. `MOV` sequence row (`FOR UPDATE`)
8. **Idempotency:**
   - `POST /api/v1/orders/{id}/returns` mandates the `Idempotency-Key` header.
   - Retrying with the same key returns the cached response without duplicate stock movements or return records.

---

## 3. Database Schema Specification (Flyway V17)

```sql
-- V17__create_customer_returns_tables.sql

-- 1. Update material_movements check constraint to allow CUSTOMER_RETURN to WASTAGE
ALTER TABLE material_movements DROP CONSTRAINT ck_material_movements_type_locations;
ALTER TABLE material_movements ADD CONSTRAINT ck_material_movements_type_locations CHECK (
    (movement_type = 'OPENING_BALANCE' AND from_location IS NULL
        AND to_location IN ('RAW_STOCK', 'INTERNAL_WIP', 'EXTERNAL_WIP', 'FINISHED_STOCK'))
    OR (movement_type = 'RECEIPT' AND from_location = 'SUPPLIER' AND to_location = 'RAW_STOCK')
    OR (movement_type = 'PURCHASE_RETURN' AND from_location = 'RAW_STOCK' AND to_location = 'RETURNED')
    OR (movement_type = 'DIRECT_SALE' AND from_location = 'RAW_STOCK' AND to_location = 'CUSTOMER')
    OR (movement_type = 'CONSUMPTION' AND from_location = 'RAW_STOCK' AND to_location = 'INTERNAL_WIP')
    OR (movement_type = 'PRODUCTION_OUTPUT' AND from_location = 'INTERNAL_WIP' AND to_location = 'FINISHED_STOCK')
    OR (movement_type = 'WASTAGE' AND from_location IN ('RAW_STOCK', 'INTERNAL_WIP', 'EXTERNAL_WIP')
        AND to_location = 'WASTAGE')
    OR (movement_type = 'OUTSOURCE_ISSUE' AND from_location = 'RAW_STOCK' AND to_location = 'EXTERNAL_WIP')
    OR (movement_type = 'OUTSOURCE_RECEIPT' AND from_location = 'EXTERNAL_WIP' AND to_location = 'FINISHED_STOCK')
    OR (movement_type = 'DELIVERY' AND from_location = 'FINISHED_STOCK' AND to_location = 'CUSTOMER')
    OR (movement_type = 'CUSTOMER_RETURN' AND from_location = 'CUSTOMER'
        AND to_location IN ('FINISHED_STOCK', 'RAW_STOCK', 'WASTAGE'))
    OR (movement_type = 'ADJUSTMENT' AND from_location IS NOT NULL
        AND (from_location IN ('RAW_STOCK', 'INTERNAL_WIP', 'EXTERNAL_WIP', 'FINISHED_STOCK')
             OR to_location IN ('RAW_STOCK', 'INTERNAL_WIP', 'EXTERNAL_WIP', 'FINISHED_STOCK')))
);

-- 2. customer_returns table: append-only return notes
CREATE TABLE customer_returns (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    return_number      VARCHAR(30)    NOT NULL,                       -- CRN-2026-0001
    order_id           UUID           NOT NULL,
    order_item_id      UUID           NOT NULL,
    return_date        DATE           NOT NULL,
    returned_weight_kg NUMERIC(12, 3) NOT NULL,
    usable             BOOLEAN        NOT NULL DEFAULT true,
    reason             VARCHAR(255)   NOT NULL,
    notes              TEXT,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by         UUID,
    version            BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT uq_customer_returns_return_number UNIQUE (return_number),
    CONSTRAINT fk_customer_returns_order FOREIGN KEY (order_id) REFERENCES customer_orders (id),
    CONSTRAINT fk_customer_returns_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id),
    CONSTRAINT ck_customer_returns_weight CHECK (returned_weight_kg > 0),
    CONSTRAINT ck_customer_returns_reason CHECK (length(trim(reason)) > 0),
    CONSTRAINT fk_customer_returns_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE INDEX ix_customer_returns_order_id ON customer_returns (order_id);
CREATE INDEX ix_customer_returns_order_item_id ON customer_returns (order_item_id);
CREATE INDEX ix_customer_returns_return_date ON customer_returns (return_date);
CREATE INDEX ix_customer_returns_created_by ON customer_returns (created_by);

-- 3. Immutability trigger
CREATE TRIGGER trg_customer_returns_immutable BEFORE UPDATE OR DELETE ON customer_returns
    FOR EACH ROW EXECUTE FUNCTION prevent_row_modification();

-- 4. Role privilege restriction
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'nexora_app') THEN
        REVOKE UPDATE, DELETE, TRUNCATE ON customer_returns FROM nexora_app;
    END IF;
END
$$;
```

---

## 4. API Specification

### Endpoint: `POST /api/v1/orders/{id}/returns`
- **Method:** `POST`
- **Path:** `/api/v1/orders/{id}/returns`
- **Security:** `@PreAuthorize("hasAuthority('ORDER_MANAGE')")`
- **Idempotency:** `@Idempotent` (Header `Idempotency-Key` required)
- **Status:** `201 Created` with `Location: /api/v1/orders/{id}/returns/{returnId}`

#### Request Body (`RecordCustomerReturnRequest`)
```json
{
  "orderItemId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "returnDate": "2026-10-18",
  "returnedWeightKg": 5.000,
  "usable": true,
  "reason": "Border reed mark defect",
  "notes": "Customer requested credit on next invoice"
}
```

#### Response Body (`CustomerReturnResponse`)
```json
{
  "id": "7fa85f64-5717-4562-b3fc-2c963f66afb7",
  "returnNumber": "CRN-2026-0001",
  "orderId": "1fa85f64-5717-4562-b3fc-2c963f66afc8",
  "orderItemId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "lineNo": 1,
  "productType": "SILK_WARP",
  "returnDate": "2026-10-18",
  "returnedWeightKg": 5.000,
  "ratePerKg": 4850.00,
  "creditedAmount": 24250.00,
  "usable": true,
  "movementNumber": "MOV-2026-0042",
  "reason": "Border reed mark defect",
  "notes": "Customer requested credit on next invoice",
  "createdAt": "2026-10-18T13:30:00Z"
}
```

### Endpoint: `GET /api/v1/orders/{id}/returns`
- **Method:** `GET`
- **Security:** `@PreAuthorize("hasAuthority('ORDER_VIEW')")`
- **Status:** `200 OK`
- **Response:** `List<CustomerReturnResponse>`

---

## 5. Desktop Client UI Specification

1. **Order Detail Screen (`OrderDetailScreen.kt`):**
   - Action Bar: When order status is `DELIVERED` or `COMPLETED`, display `Return` button (primary/outlined) enabled for users with `ORDER_MANAGE` permission.
   - Click triggers `ReturnOrderDialog`.
   - Body Section: Add "Customer Returns" table below Order Items listing all recorded returns (`CRN`, Date, Item/Product, Weight kg, Usable status chip, Credited Amount, Reason).
2. **Return Dialog (`RecordCustomerReturnDialog.kt`):**
   - Item dropdown listing order items with remaining delivered weight.
   - Date picker defaulting to current business date.
   - Quantity input (kg, numeric with 3 decimals, validating $> 0$ and $\le$ remaining weight).
   - "Usable Material" switch (Default true: "Usable for resale" / false: "Unusable - Damaged").
   - Reason input (required).
   - Notes input (optional).
   - Validation & submission with `Idempotency-Key` generation.

---

## 6. Verification & Test Plan

1. **Unit Tests:**
   - `MovementRulesTest`: Assert legal pairs include `CUSTOMER -> FINISHED_STOCK`, `CUSTOMER -> RAW_STOCK`, `CUSTOMER -> WASTAGE`.
   - `CustomerReturnTest`: Validate entity creation, weight validation, credit amount calculation.
2. **Repository & Schema Tests:**
   - `ForeignKeyIndexIntegrationTest`: Ensure all foreign keys on `customer_returns` are indexed and follow conventions (`fk_`, `uq_`, `ck_`).
   - `DerivedValuesTest`: Verify no forbidden derived columns in `customer_returns`.
   - `ImmutabilityTriggerIntegrationTest`: Verify `customer_returns` UPDATE and DELETE are rejected by trigger.
   - `MigrationIntegrationTest`: Test Flyway migration V17 against empty DB and existing data.
3. **API Integration Tests (`CustomerReturnApiTest`):**
   - Prove successful return for usable warp ($\to$ `FINISHED_STOCK`).
   - Prove successful return for unusable warp ($\to$ `WASTAGE`).
   - Prove rejection if order is not `DELIVERED`/`COMPLETED` (409 `INVALID_STATE_TRANSITION`).
   - Prove rejection if return weight exceeds delivered weight (409 `INSUFFICIENT_INVENTORY`).
   - Prove rejection if non-positive weight or blank reason (400 `VALIDATION_FAILED`).
   - Prove idempotency replay returns identical response.
   - Prove security: 401 unauthenticated, 403 STAFF.
4. **Financial Integration Test:**
   - Prove customer outstanding decreases by $\text{returnedWeightKg} \times \text{ratePerKg}$.
5. **Desktop Client Tests:**
   - View model tests for `RecordCustomerReturnViewModel` verifying input validation, item selection, and idempotent repository calls.
