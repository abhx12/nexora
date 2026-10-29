# OpenSpec: Customer-Specific Credit Period & Overdue Tracking (FR-PARTY-17, E-PARTY-08)

- **Feature ID:** FEAT-CUSTOMER-CREDIT-TERMS
- **Requirements Covered:** FR-PARTY-17, NFR-DATA-01, NFR-DATA-02, NFR-SEC-09, NFR-USE-01
- **Milestone:** M13
- **Status:** APPROVED (Spec-Driven Development)
- **Author:** Antigravity Pair Programmer
- **Date:** 2026-10-23

---

## 1. Executive Summary & Business Motivation

Silk yarn customers typically negotiate varying payment credit terms (e.g., 15, 30, 45 days).
Previously, SmartSilk only grouped outstanding customer orders into broad chronological age brackets (0–30, 31–60, 61–90, 90+ days) without knowing each customer's agreed terms, leaving it to the owner to manually recall which customers were genuinely overdue.

This specification defines the complete implementation of **Customer-Specific Credit Terms & Overdue Tracking**:
1. Database schema migration `V20__add_customer_credit_period.sql` adding `customer_credit_days` to the `parties` table with range constraints (0 to 365 days).
2. Domain entity & DTO updates:
   - `Party.updateCustomerTerms(Integer creditPeriodDays)` validating that the party holds the `CUSTOMER` role.
   - Update `PartyResponse` and `PartyFields` to include `customerCreditDays`.
3. REST Endpoint:
   - `PATCH /api/v1/parties/{id}/customer-terms` (`PARTY_MANAGE`), accepting `{"creditPeriodDays": 30}`.
4. Overdue Evaluation:
   - In `CustomerOutstandingResponse`, open orders for a customer with configured credit days derive `creditDays`, `dueDate = orderDate + creditDays`, and `isOverdue = today > dueDate`.
   - Customer total summary indicates if the customer has overdue obligations.
5. Desktop Client:
   - Edit and view customer credit terms in party details and forms.
   - Visual overdue chip/indicator on the customer outstanding view for late orders.

---

## 2. Database Schema (Flyway V20)

```sql
-- V20__add_customer_credit_period.sql
-- Add customer_credit_days column to parties table

ALTER TABLE parties ADD COLUMN customer_credit_days INTEGER;
ALTER TABLE parties ADD CONSTRAINT ck_parties_customer_credit_days
    CHECK (customer_credit_days IS NULL OR (customer_credit_days >= 0 AND customer_credit_days <= 365));
```

---

## 3. API Contract

### `PATCH /api/v1/parties/{id}/customer-terms`
- **Permission:** `PARTY_MANAGE`
- **Request Body:**
```json
{
  "creditPeriodDays": 30
}
```
- **Validation:**
  - `@PositiveOrZero @Max(365)`
  - Party must exist (`404 RESOURCE_NOT_FOUND`).
  - Party must have `CUSTOMER` role (`400 VALIDATION_FAILED` / `FieldValidationException`).
- **Response:** `200 OK` with full `PartyResponse` containing updated `customerCreditDays`.

---

## 4. Desktop Client Contract
- `PartyDtos.kt`: Add `customerCreditDays: Int?` to `PartyResponse`, `PartyFormState`, and `CustomerTermsRequest`.
- Party detail & customer role tab: Displays agreed credit period; enables editing.
- Customer Outstanding: Flags orders where `isOverdue == true`.
