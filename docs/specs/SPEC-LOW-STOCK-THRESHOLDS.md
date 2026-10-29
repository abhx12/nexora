# OpenSpec: Low-Stock Alerts & Inventory Thresholds (FR-INV-18, FR-PROC-20, FR-NTF-07)

- **Feature ID:** FEAT-INV-THRESHOLDS
- **Requirements Covered:** FR-INV-18, FR-PROC-20, FR-NTF-07, NFR-DATA-01, NFR-DATA-02, NFR-DATA-05, NFR-SEC-09, NFR-USE-01
- **Milestone:** M11
- **Status:** APPROVED (Spec-Driven Development)
- **Author:** Antigravity Pair Programmer
- **Date:** 2026-10-18

---

## 1. Executive Summary & Business Motivation

In the family silk-yarn trading and warping enterprise, uninterrupted operations depend on continuous availability of raw materials (chiefly `RAW_SILK` in `RAW_STOCK`) and warp stock (`SILK_WARP` and `KORA_WARP` in `FINISHED_STOCK` or `INTERNAL_WIP`). If raw silk runs out, twisting and warping machinery halts and workers sit idle; if finished warp runs dry, customer deliveries are delayed.

Previously, stock quantities were displayed in `GET /inventory/summary`, but operators had no systematic way to:
1. Define minimum operating stock thresholds for critical products and locations.
2. Instantly see whether stock is at or below safe operational levels (low-stock warning status).
3. Receive automated daily alert notifications (`LOW_STOCK`) prompting timely raw silk procurement.
4. View low-stock warning indicators when making procurement decisions.

This specification defines the complete vertical implementation of **Inventory Thresholds and Low-Stock Alerting**:
- Configurable minimum stock thresholds (`min_threshold_kg >= 0`) per legal `(product_type, location)` pair stored in `inventory_thresholds`.
- Direct integration into `GET /api/v1/inventory/summary` (`minThresholdKg` and `isLowStock` on each balance line).
- Dedicated queries `GET /api/v1/inventory/thresholds` and `GET /api/v1/inventory/low-stock`.
- Automated daily `LOW_STOCK` notifications with deduplication via `notifications`.
- Compose Desktop UI integration: visual warning badges, threshold configuration dialog, and procurement hints.

---

## 2. Business Invariants & Domain Rules

1. **Legal Product and Location Pairs (Domain Rule §4):**
   Thresholds may only be configured for legal internal inventory locations:
   - `RAW_STOCK` with `RAW_SILK` or `VUDA_WARP`
   - `INTERNAL_WIP`, `EXTERNAL_WIP`, `FINISHED_STOCK` with `SILK_WARP` or `KORA_WARP`
   Any invalid pair submitted to the API is strictly rejected with 400 `VALIDATION_FAILED` (or 422 `MALFORMED_REQUEST`).

2. **Non-Negative Weight Invariant:**
   $\text{min\_threshold\_kg} \ge 0.000$ with scale 3 (`HALF_UP`). Negative thresholds are rejected with 400 `VALIDATION_FAILED`. Setting a threshold to 0.000 effectively disables alerting for that pair.

3. **Low-Stock Evaluation:**
   An item is marked `isLowStock = true` if and only if:
   $$\text{threshold} \text{ exists} \land \text{min\_threshold\_kg} > 0 \land \text{current\_quantity\_kg} \le \text{min\_threshold\_kg}$$
   The deficit is computed as $\text{deficit\_kg} = \text{min\_threshold\_kg} - \text{current\_quantity\_kg}$.

4. **Security & Authorization:**
   - Reading thresholds and low-stock status: `INVENTORY_VIEW` (accessible by both `OWNER` and `STAFF`).
   - Modifying or deleting thresholds: `INVENTORY_ADJUST` or `INVENTORY_MANAGE` (restricted to `OWNER`). Unauthenticated requests yield 401; unauthorized staff yield 403 `ACCESS_DENIED`.

5. **Notification Deduplication:**
   Automated low-stock alerts use deduplication key:
   $$\text{LOW\_STOCK}:\text{productType}:\text{location}:\text{todayDate}$$
   Ensuring only one notification per low-stock item per business date, avoiding alert fatigue.

---

## 3. Database Schema Specification (Flyway V18)

```sql
-- Inventory thresholds for low-stock warnings and alerts (FR-INV-18, FR-PROC-20, FR-NTF-07).
CREATE TABLE inventory_thresholds (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_type     VARCHAR(20)    NOT NULL,
    location         VARCHAR(20)    NOT NULL,
    min_threshold_kg NUMERIC(12, 3) NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by       UUID,
    version          BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT uq_inventory_thresholds_product_location UNIQUE (product_type, location),
    CONSTRAINT ck_inventory_thresholds_product CHECK (product_type IN ('RAW_SILK', 'SILK_WARP', 'KORA_WARP', 'VUDA_WARP')),
    CONSTRAINT ck_inventory_thresholds_location CHECK (location IN ('RAW_STOCK', 'INTERNAL_WIP', 'EXTERNAL_WIP', 'FINISHED_STOCK')),
    CONSTRAINT ck_inventory_thresholds_min_weight CHECK (min_threshold_kg >= 0),
    CONSTRAINT ck_inventory_thresholds_location_product CHECK (
        (location = 'RAW_STOCK' AND product_type IN ('RAW_SILK', 'VUDA_WARP'))
        OR (location IN ('INTERNAL_WIP', 'EXTERNAL_WIP', 'FINISHED_STOCK') AND product_type IN ('SILK_WARP', 'KORA_WARP'))),
    CONSTRAINT fk_inventory_thresholds_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);
CREATE INDEX ix_inventory_thresholds_created_by ON inventory_thresholds (created_by);

-- Seed notification configuration for LOW_STOCK if not already present
INSERT INTO notification_configurations (notification_type, enabled, days_before_due, overdue_repeat_days, priority)
VALUES ('LOW_STOCK', TRUE, 0, 1, 'HIGH')
ON CONFLICT (notification_type) DO NOTHING;
```

---

## 4. API Contract & Endpoints

### 4.1 Endpoints Summary

| Method | Path | Authority | Purpose |
|---|---|---|---|
| `GET` | `/api/v1/inventory/thresholds` | `INVENTORY_VIEW` | List all configured thresholds |
| `PUT` | `/api/v1/inventory/thresholds/{productType}/{location}` | `INVENTORY_ADJUST` | Set / update min stock threshold |
| `DELETE` | `/api/v1/inventory/thresholds/{productType}/{location}` | `INVENTORY_ADJUST` | Delete threshold |
| `GET` | `/api/v1/inventory/low-stock` | `INVENTORY_VIEW` | List all currently breached stock items |
| `GET` | `/api/v1/inventory/summary` | `INVENTORY_VIEW` | Existing summary, enriched with threshold & low-stock flag |

### 4.2 DTO Definitions

```json
// Threshold request
{
  "minThresholdKg": 50.000
}

// Threshold response
{
  "id": "...",
  "productType": "RAW_SILK",
  "location": "RAW_STOCK",
  "minThresholdKg": 50.000,
  "createdAt": "2026-10-18T10:00:00Z",
  "updatedAt": "2026-10-18T10:00:00Z"
}

// Low-stock item response
{
  "productType": "RAW_SILK",
  "location": "RAW_STOCK",
  "currentQuantityKg": 25.000,
  "minThresholdKg": 50.000,
  "deficitKg": 25.000
}
```

---

## 5. Desktop Client UI Specification

1. **Inventory Summary Screen (`InventoryScreens.kt`):**
   - Summary cards & Table lines render an amber/red warning pill `LOW STOCK` when `isLowStock == true`.
   - Display `Threshold: X.XXX kg` under the quantity where configured.
   - An action button "Configure Thresholds" opens the threshold management dialog.
2. **Threshold Configuration Dialog (`InventoryThresholdDialog.kt`):**
   - Lists the 8 legal product-location combinations with their current balance and configurable threshold.
   - Allows quick editing and saving with validation.
3. **Procurement Screen Hint (`PurchaseScreens.kt`):**
   - Displays a prominent low-stock notice on raw silk when creating a purchase order if `RAW_SILK` in `RAW_STOCK` is in deficit.

---

## 6. Verification & Test Plan

1. **Database Schema:** `ForeignKeyIndexIntegrationTest`, `DerivedValuesTest`, `MigrationIntegrationTest`.
2. **API Tests:** `InventoryThresholdApiTest`:
   - Setting a threshold with positive kg returns 200/201.
   - Setting negative kg returns 400.
   - Setting invalid product/location returns 400.
   - Staff cannot set threshold (403), unauthenticated gets 401.
   - Getting low-stock list returns only items below threshold.
   - Summary balance includes threshold and `isLowStock` indicator.
3. **Notification Job:** `LowStockAlertJobTest` proves that a breached threshold creates a `LOW_STOCK` notification with correct dedupe key, and repeat runs on the same date create nothing.
4. **Desktop UI Tests:** `InventoryViewModelTest` and `InventoryScreensTest` verify threshold display and dialog interactions.
