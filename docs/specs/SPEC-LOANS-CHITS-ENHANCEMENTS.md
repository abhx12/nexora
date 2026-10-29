# OpenSpec: Loans & Chits Enhancements (FR-LOAN-10, FR-CHIT-10, E-LOAN-09, E-CHIT-09)

- **Feature ID:** FEAT-LOANS-CHITS-ENHANCEMENTS
- **Requirements Covered:** FR-LOAN-10, FR-CHIT-10, E-LOAN-09, E-CHIT-09, NFR-DATA-01, NFR-SEC-09, NFR-USE-01
- **Milestone:** M17
- **Status:** APPROVED (Spec-Driven Development)
- **Author:** Antigravity Pair Programmer
- **Date:** 2026-10-23

---

## 1. Executive Summary & Business Motivation

In the traditional textile business in Tamil Nadu, liquidity and capital rely heavily on private borrowings/bank loans and chit funds:
1. **Loan Repayment Advice (FR-LOAN-10, E-LOAN-09):** The owner regularly needs to decide how to allocate available cash flow. Should they just pay monthly interest, pay down a slice of principal, or close the loan completely to eliminate ongoing interest burden? Computing the monthly interest and evaluating available liquidity after each option without manual spreadsheets prevents costly interest accrual while ensuring working capital is not depleted.
2. **Chit Auction Bidding & Dividend Calculation (FR-CHIT-10, E-CHIT-09):** Every month in a chit fund, members bid a discount to take the lump sum pot. The bid discount is distributed equally as a "dividend" among all members, reducing that month's installment contribution below the standard monthly amount. The owner needs a precise calculation of the net contribution due given the auction bid.

---

## 2. Domain Rules & Specifications

### 2.1 Loan Repayment Advice (`GET /api/v1/loans/{id}/advice`)
- **Endpoint:** `GET /api/v1/loans/{id}/advice`
- **Permission:** `LOAN_MANAGE`
- **Invariants:**
  1. Read-only: this endpoint calculates options and returns advice. It **never** creates transactions or mutates state (FR-LOAN-10 AC2).
  2. If the loan is `CLOSED` or outstanding principal is `0.00`, `interestDueSuggestion` is `0.00` and closure option indicates already settled.
  3. **Interest Suggestion Calculation:**
     - If `loan.getInterestRatePercent()` is present and $> 0$, monthly rate $r = \text{interestRatePercent} / 100$.
     - If `interestRatePercent` is null/0, check `interestNotes` for a pattern like `"X%"` or `"X percent"`; if found, use $X / 100$; otherwise fallback to standard monthly rate or 0.00.
     - Monthly interest suggestion: $\text{round}(\text{outstandingPrincipal} \times r)$.
  4. **Available Funds:**
     - Query total available balances across all active financial accounts (`CASH` + `BANK`).
  5. **Generated Advice Options:**
     - `PAY_INTEREST`: Pay suggested interest due. Amount = `interestDueSuggestion`. `availableAfter` = `availableFunds - amount`.
     - `PART_PRINCIPAL`: Pay 20% of outstanding principal (rounded to nearest 1,000, capped at available funds and outstanding). `availableAfter` = `availableFunds - amount`.
     - `FULL_CLOSURE`: Pay full outstanding principal + suggested interest. Amount = `outstandingPrincipal + interestDueSuggestion`. `availableAfter` = `availableFunds - amount`.
- **Response Structure (`LoanAdviceResponse`):**
  - `loanId`: UUID
  - `loanNumber`: String
  - `outstandingPrincipal`: BigDecimal
  - `interestRatePercent`: BigDecimal (nullable)
  - `interestDueSuggestion`: BigDecimal
  - `totalAvailableFunds`: BigDecimal
  - `options`: List of `LoanAdviceOption`:
    - `action`: String (`PAY_INTEREST`, `PART_PRINCIPAL`, `FULL_CLOSURE`)
    - `amount`: BigDecimal
    - `availableAfter`: BigDecimal
    - `description`: String

### 2.2 Chit Auction Bidding & Dividend Calculation (`POST /api/v1/chits/{id}/bids`)
- **Endpoint:** `POST /api/v1/chits/{id}/bids`
- **Permission:** `CHIT_MANAGE`
- **Request Structure (`ChitBidRequest`):**
  - `month`: String, format `"yyyy-MM"` (e.g. `"2027-03"`)
  - `bidAmount`: BigDecimal $\ge 0.00$
- **Invariants & Validations:**
  1. Chit must exist (else 404 `RESOURCE_NOT_FOUND`).
  2. Chit must not be `CLOSED` (if `CLOSED`, return 409 `INVALID_STATE_TRANSITION`).
  3. `bidAmount` must be $\le \text{chit.totalAmount}$ (if $> \text{totalAmount}$, return 400 `VALIDATION_FAILED` per FR-CHIT-10 AC2).
  4. Formula:
     - $\text{durationMonths} = \text{chit.getDurationMonths()}$
     - $\text{standardMonthly} = \text{chit.getMonthlyAmount()}$
     - $\text{dividendPerMember} = \text{round}(\text{bidAmount} / \text{durationMonths})$
     - $\text{contributionDue} = \max(0.00, \text{standardMonthly} - \text{dividendPerMember})$
- **Response Structure (`ChitBidResponse`):**
  - `chitId`: UUID
  - `chitNumber`: String
  - `month`: String
  - `bidAmount`: BigDecimal
  - `durationMonths`: int
  - `dividendPerMember`: BigDecimal
  - `standardMonthlyAmount`: BigDecimal
  - `contributionDue`: BigDecimal

---

## 3. Desktop Client Integration
- Provide DTOs and client calls in `LoanRepository` and `ChitRepository`.
- Display repayment advice in `LoanDetailScreen`.
- Display bid and dividend calculation dialog in `ChitDetailScreen`.
