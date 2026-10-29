# OpenSpec: Analytics & Profitability Reports (FR-RPT-17, FR-WORK-10, FR-OUT-12, FR-PROD-12, E-WORK-05, E-RPT-15, E-RPT-16)

- **Feature ID:** FEAT-ANALYTICS-AND-PROFITABILITY
- **Requirements Covered:** FR-RPT-17, FR-WORK-10, FR-OUT-12, FR-PROD-12, E-WORK-05, E-RPT-15, E-RPT-16, NFR-PERF-07, NFR-SEC-09, NFR-USE-01
- **Milestone:** M18
- **Status:** APPROVED (Spec-Driven Development)
- **Author:** Antigravity Pair Programmer
- **Date:** 2026-10-23

---

## 1. Executive Summary & Business Motivation

While the MVP provided essential operational ledger accounting (cash positions, customer outstandings, supplier payables, sales volume, and expenses), modern textile business management requires strategic insights:
1. **Worker Efficiency (FR-WORK-10, E-WORK-05):** Measuring rolling and warping productivity ($kg/\text{hour}$) per worker over any date range to optimize shift assignments and fair wage rates.
2. **Profit & Loss (FR-RPT-17, E-RPT-15):** Aggregated monthly or yearly P&L statement tracking delivered sales revenue against production costs (materials, wages, outsourcing), operating business expenses, and financing costs (loan interest), strictly excluding personal drawings.
3. **Multi-Dimensional Profitability (FR-RPT-17, FR-OUT-12, FR-PROD-12, E-RPT-16):** Profit margin and margin percentage broken down by Product line (`SILK_WARP`, `KORA_WARP`, `RAW_SILK`), by Customer, and by Outsourcing Manufacturer.

---

## 2. API Specifications & Contracts

### 2.1 Worker Efficiency Report (`GET /api/v1/reports/worker-efficiency`)
- **Permission:** `REPORT_VIEW`
- **Parameters:**
  - `from` (`LocalDate`, required)
  - `to` (`LocalDate`, required, must be $\ge$ `from`)
- **Output:** `List<WorkerEfficiencyRow>`
  - `worker`: `PartyRef(id, name)`
  - `hours`: BigDecimal (scale 2)
  - `kg`: BigDecimal (scale 3)
  - `kgPerHour`: BigDecimal (scale 2)

### 2.2 Profit & Loss Report (`GET /api/v1/reports/profit-and-loss`)
- **Permission:** `REPORT_VIEW`
- **Parameters:**
  - `from` (`LocalDate`, required)
  - `to` (`LocalDate`, required, must be $\ge$ `from`)
  - `groupBy` (`SalesGrouping`: `MONTH` or `YEAR`, default `MONTH`)
- **Output:** `List<ProfitAndLossRow>`
  - `period`: String (`yyyy-MM` or `yyyy`)
  - `revenue`: BigDecimal (Delivered sales orders in period)
  - `businessExpenses`: BigDecimal (Operating expenses with `kind = 'BUSINESS'`)
  - `interestCost`: BigDecimal (Loan interest paid in period)
  - `productionCost`: BigDecimal (Raw materials + worker wages + outsourcing charges)
  - `profit`: BigDecimal (`revenue - businessExpenses - interestCost - productionCost`)

### 2.3 Profitability Report (`GET /api/v1/reports/profitability`)
- **Permission:** `REPORT_VIEW`
- **Parameters:**
  - `from` (`LocalDate`, required)
  - `to` (`LocalDate`, required)
  - `dimension` (`ProfitabilityDimension`: `PRODUCT`, `CUSTOMER`, `MANUFACTURER`)
- **Output:** `List<ProfitabilityRow>`
  - `key`: String (product name, customer name, or manufacturer name)
  - `id`: UUID (nullable, party id if customer or manufacturer)
  - `revenue`: BigDecimal
  - `cost`: BigDecimal
  - `margin`: BigDecimal (`revenue - cost`)
  - `marginPercent`: BigDecimal (`margin / revenue * 100`, scale 2, 0.00 if revenue is 0)

---

## 3. Desktop Client Integration
- Provide DTOs, repository methods, and viewmodel integration for:
  - Worker efficiency report view
  - Profit and Loss statement view
  - Profitability analysis tabs
