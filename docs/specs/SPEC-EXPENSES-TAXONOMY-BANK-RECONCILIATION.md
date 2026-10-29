# Specification: Configurable Expense Categories and Bank Statement Reconciliation

**Milestone:** M16  
**Requirements:** FR-FIN-19, FR-FIN-20  
**API Endpoints:** E-FIN-17 (`/expense-categories`), E-FIN-18 (`/finance/accounts/{id}/statement-imports`)  
**Status:** In Progress

---

## 1. Overview & Business Value

1. **Configurable Expense Categories (FR-FIN-19, E-FIN-17):**
   - In MVP, expense categories were restricted to a fixed 7-item enum (`TRANSPORT`, `ELECTRICITY`, `RENT`, `WORKER`, `MAINTENANCE`, `PACKING`, `OTHER`).
   - Saree manufacturing businesses often require domain-specific categories such as `FUEL` (generator/delivery van), `TEA_SNACKS`, `DYES_CHEMICALS`, `STATIONERY`, `LEGAL_AUDIT`.
   - Owners can configure new categories via `POST /api/v1/expense-categories` with upper snake-case validation (`^[A-Z_]{2,30}$`).
   - Duplicate names return 409 `DUPLICATE_RESOURCE`.
   - Expenses recorded with unknown categories return 400 `MALFORMED_REQUEST`.

2. **Bank Statement Import & Reconciliation (FR-FIN-20, E-FIN-18):**
   - Business owners download bank statements (CSV format) from net-banking portals (SBI, HDFC, Canara, ICICI).
   - `POST /api/v1/finance/accounts/{id}/statement-imports` accepts a statement file, parses rows (date, description, amount, direction), and suggests matching financial transactions from the account's ledger based on matching amount, direction, and date window.
   - Returns 202 `ACCEPTED` with summary of detected rows and suggested matches.
   - Unparseable or empty files return 400 `MALFORMED_REQUEST`.

---

## 2. Database Schema Changes (`V22`)

```sql
-- Table for configurable expense categories
CREATE TABLE expense_categories (
    id UUID PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed initial standard categories
INSERT INTO expense_categories (id, name, active, created_at) VALUES
    (gen_random_uuid(), 'TRANSPORT', true, NOW()),
    (gen_random_uuid(), 'ELECTRICITY', true, NOW()),
    (gen_random_uuid(), 'RENT', true, NOW()),
    (gen_random_uuid(), 'WORKER', true, NOW()),
    (gen_random_uuid(), 'MAINTENANCE', true, NOW()),
    (gen_random_uuid(), 'PACKING', true, NOW()),
    (gen_random_uuid(), 'OTHER', true, NOW());

-- Widen expenses category and remove static enum check
ALTER TABLE expenses DROP CONSTRAINT ck_expenses_category;
ALTER TABLE expenses ALTER COLUMN category TYPE VARCHAR(30);
```

---

## 3. Endpoints & API Contract

### E-FIN-17: Expense Categories
- `POST /api/v1/expense-categories`
  - Permission: `EXPENSE_CREATE`
  - Request: `{"name": "FUEL"}`
  - Response: 201 `{"id": "...", "name": "FUEL", "active": true}`
  - 409 `DUPLICATE_RESOURCE` on duplicate name.
  - 400 `VALIDATION_FAILED` on invalid name format.
- `GET /api/v1/expense-categories`
  - Permission: `EXPENSE_VIEW` or `EXPENSE_CREATE`
  - Response: 200 `[{"id": "...", "name": "TRANSPORT", "active": true}, ...]`

### E-FIN-18: Bank Statement Import
- `POST /api/v1/finance/accounts/{id}/statement-imports`
  - Permission: `FINANCE_MANAGE`
  - Multipart file: `file`
  - Response: 202 `ACCEPTED`
    ```json
    {
      "importId": "uuid",
      "accountId": "uuid",
      "totalRows": 15,
      "suggestedMatches": 12,
      "items": [
        {
          "statementDate": "2026-10-20",
          "description": "UPI/9840012345/KUMAR SILKS",
          "amount": 45000.00,
          "direction": "IN",
          "matchedTransactionId": "uuid-or-null"
        }
      ]
    }
    ```
  - Errors: 400 `MALFORMED_REQUEST` (empty or corrupt file), 404 (account not found), 422 (not a BANK account).
