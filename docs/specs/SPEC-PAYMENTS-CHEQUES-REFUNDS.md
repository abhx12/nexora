# Specification: Customer Refunds & Cheque Lifecycle (M15)

**Milestone:** M15
**Requirements:** FR-PAY-25, E-PAY-13, E-PAY-14
**Date:** 2026-10-23
**Status:** In Progress

---

## 1. Overview & Objectives

In silk and textile trade, customers frequently pay in advance (before warp delivery) or settle via bank cheques. When orders are cancelled or amended, unallocated customer advances must be refunded safely from cash or bank accounts without breaking double-entry ledger invariants. Furthermore, cheques are not immediately cleared funds; cheques should optionally be recorded in a `PENDING` state where obligations are reserved to prevent double settlement, while available cash/bank balances remain unaffected until clearance or bounce.

This milestone implements:
1. **Customer Advance Refunds (E-PAY-13)**: `POST /api/v1/payments/{id}/refunds` to return unallocated funds to a customer.
2. **Cheque Clearance & Bounce (E-PAY-14)**:
   - Pending cheque entry (`RecordPaymentRequest.pendingCheque = true`).
   - `POST /api/v1/payments/{id}/cheque-clearance` to clear pending cheques into bank balances.
   - `POST /api/v1/payments/{id}/cheque-bounce` to bounce cheques, releasing allocations back to outstanding obligations and reversing cleared money if applicable.
3. **Desktop UI Integration**: Clearance, bounce, and refund action buttons with confirmation dialogs on the Payment Detail screen.

---

## 2. API Design & Endpoints

### 2.1 E-PAY-13: Refund to a Customer
- **Method & Route:** `POST /api/v1/payments/{id}/refunds`
- **Security:** `PAYMENT_CREATE`, `@Idempotent` (Idempotency-Key required)
- **Request Body:**
  ```json
  {
    "amount": 5000.00,
    "paymentMethod": "UPI",
    "financialAccountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "refundDate": "2026-10-23",
    "reason": "Order cancelled by customer"
  }
  ```
- **Response:** `201 Created` with `PaymentResponse` representing the refund payment (`direction = PAID`, `purpose = CUSTOMER`, `referenceNumber = "Refund for PAY-..."`).
- **Validation & Errors:**
  - `404 RESOURCE_NOT_FOUND` if payment `{id}` does not exist.
  - `409 INVALID_STATE_TRANSITION` if payment `{id}` is not `purpose = CUSTOMER` or is not in `RECORDED` status.
  - `409 PAYMENT_ALLOCATION_EXCEEDED` if `amount` > remaining unallocated advance on the payment.
  - `409 INSUFFICIENT_FUNDS` if the source financial account has insufficient balance for an `OUT` transaction.

### 2.2 E-PAY-14: Cheque Clearance
- **Method & Route:** `POST /api/v1/payments/{id}/cheque-clearance`
- **Security:** `PAYMENT_CREATE`
- **Request Body:**
  ```json
  {
    "date": "2026-10-24",
    "notes": "Cleared via clearing house"
  }
  ```
- **Response:** `200 OK` with updated `PaymentResponse` (`status = RECORDED`).
- **Validation & Errors:**
  - `409 INVALID_STATE_TRANSITION` if payment method is not `CHEQUE` or status is not `PENDING`.

### 2.3 E-PAY-14: Cheque Bounce
- **Method & Route:** `POST /api/v1/payments/{id}/cheque-bounce`
- **Security:** `PAYMENT_CREATE`
- **Request Body:**
  ```json
  {
    "date": "2026-10-24",
    "notes": "Insufficient funds in drawer account"
  }
  ```
- **Response:** `200 OK` with updated `PaymentResponse` (`status = BOUNCED`).
- **Validation & Errors:**
  - `409 INVALID_STATE_TRANSITION` if payment method is not `CHEQUE` or status is already `BOUNCED` or `REVERSED`.
- **Behavior:**
  - Allocations on the payment are released (excluded from outstanding calculation).
  - If the cheque was already `RECORDED`, the associated `financial_transactions` row is reversed.

---

## 3. Database Schema Updates (`V21`)

1. **Payment Status:**
   - Update `ck_payments_status` to accept `'RECORDED', 'REVERSED', 'PENDING', 'BOUNCED'`.
2. **Refund Tracking:**
   - Add `refund_of_payment_id UUID REFERENCES payments(id)` to `payments`.
   - Update `ck_payments_purpose_direction` to permit `purpose = 'CUSTOMER'` and `direction = 'PAID'` when `refund_of_payment_id IS NOT NULL`.
3. **Ledger Transactions:**
   - Update `ck_financial_transactions_type` to include `'CUSTOMER_REFUND'`.
   - Update `ck_financial_transactions_type_direction` to allow `'CUSTOMER_REFUND'` with `direction = 'OUT'`.
   - Update `ck_financial_transactions_type_reference` to associate `'CUSTOMER_REFUND'` with `payment_id`.
