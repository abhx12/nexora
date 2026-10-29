# OpenSpec: Loan & Chit Due Reminders (FR-NTF-08, FR-NTF-09, FR-LOAN-11, FR-CHIT-10)

- **Feature ID:** FEAT-LOAN-CHIT-REMINDERS
- **Requirements Covered:** FR-NTF-08, FR-NTF-09, FR-LOAN-11, FR-CHIT-10, NFR-DATA-01, NFR-DATA-02, NFR-SEC-09, NFR-USE-01, NFR-REL-02
- **Milestone:** M12
- **Status:** APPROVED (Spec-Driven Development)
- **Author:** Antigravity Pair Programmer
- **Date:** 2026-10-18

---

## 1. Executive Summary & Business Motivation

In the family silk-yarn enterprise, managing cash flow and financial obligations is paramount to creditworthiness and maintaining trust with private lenders, banks, and chit organisers.
While supplier payment dues (`SUPPLIER_DUE`) and low-stock alerts (`LOW_STOCK`) are automated via daily scheduled jobs, the owner previously had to mentally track or manually inspect loan repayment due dates and monthly chit fund contribution deadlines.

Missing a chit contribution deadline causes penalty interest or forfeiture of bidding eligibility; missing a loan interest or principal repayment damages the owner's standing with lenders.

This specification defines the complete vertical implementation of **Loan & Chit Due Reminders**:
1. Notification type expansion to support `LOAN_DUE` and `CHIT_DUE` in `notifications` and `notification_configurations`.
2. Database migration `V19__create_loan_and_chit_notifications.sql` updating table constraints and reference types.
3. Automated daily reminder generation for:
   - **Active Loans** with a configured `dueDate` and remaining `outstandingPrincipal > 0`.
   - **Active Chits** with upcoming or overdue monthly installment contributions (`monthlyAmount`).
4. Support for the three standard reminder kinds (`DUE_SOON`, `DUE_TODAY`, `OVERDUE`) with configurable notice horizons and repeat cadences.
5. Automatic resolution of open reminders when a loan is closed/settled or a chit contribution is paid.
6. Desktop client UI integration in notification lists, dashboard dues, and reminder configuration settings.

---

## 2. Business Invariants & Domain Rules

### 2.1 Loan Reminders (`LOAN_DUE`)
1. **Target Selection:** Only loans in `LoanStatus.ACTIVE` with non-null `dueDate` and `outstandingPrincipal > 0` are evaluated.
2. **Timeline Calculation:**
   $$\text{overdueDays} = \text{ChronoUnit.DAYS.between}(\text{dueDate}, \text{today})$$
   - $\text{overdueDays} = 0 \implies \text{ReminderKind.DUE\_TODAY}$
   - $\text{overdueDays} < 0 \land -\text{overdueDays} = \text{daysBeforeDue} \implies \text{ReminderKind.DUE\_SOON}$
   - $\text{overdueDays} > 0 \land \text{overdueDays} \pmod{\text{overdueRepeatDays}} = 0 \implies \text{ReminderKind.OVERDUE}$
3. **Deduplication Key:**
   $$\text{LOAN\_DUE}:\{\text{loanId}\}:\{\text{kind}\}(:\{\text{today}\}\text{ if OVERDUE})$$
4. **Resolution:** When a loan is closed (`LoanStatus.CLOSED`) or outstanding principal reaches 0, all open `LOAN_DUE` notifications referencing that loan are transitioned to `NotificationStatus.RESOLVED`.

### 2.2 Chit Reminders (`CHIT_DUE`)
1. **Target Selection:** Only chits in `ChitStatus.ACTIVE` where remaining expected contributions $> 0$ are evaluated.
2. **Current Installment & Due Date:**
   For installment number $K = \text{contributionsCount} + 1 \le \text{durationMonths}$:
   $$\text{installmentDueDate} = \text{startDate.plusMonths}(K - 1)$$
   Adjusting for month length (e.g. 31st on a 30-day month).
3. **Timeline Calculation:**
   $$\text{overdueDays} = \text{ChronoUnit.DAYS.between}(\text{installmentDueDate}, \text{today})$$
   - $\text{overdueDays} = 0 \implies \text{ReminderKind.DUE\_TODAY}$
   - $\text{overdueDays} < 0 \land -\text{overdueDays} = \text{daysBeforeDue} \implies \text{ReminderKind.DUE\_SOON}$
   - $\text{overdueDays} > 0 \land \text{overdueDays} \pmod{\text{overdueRepeatDays}} = 0 \implies \text{ReminderKind.OVERDUE}$
4. **Deduplication Key:**
   $$\text{CHIT\_DUE}:\{\text{chitId}\}:\text{M}\{K\}:\{\text{kind}\}(:\{\text{today}\}\text{ if OVERDUE})$$
5. **Resolution:** When an active chit receives its monthly contribution (or is closed), open reminders for that chit/installment are transitioned to `NotificationStatus.RESOLVED`.

---

## 3. Database Schema Specification (Flyway V19)

```sql
-- V19__create_loan_and_chit_notifications.sql
-- Expand notification constraints to support CHIT_DUE and CHIT reference type

ALTER TABLE notifications DROP CONSTRAINT ck_notifications_type;
ALTER TABLE notifications ADD CONSTRAINT ck_notifications_type CHECK (
    notification_type IN ('SUPPLIER_DUE', 'CUSTOMER_OUTSTANDING', 'LOAN_DUE', 'CHIT_DUE', 'LOW_STOCK', 'PENDING_OUTSOURCING')
);

ALTER TABLE notifications DROP CONSTRAINT ck_notifications_reference_type;
ALTER TABLE notifications ADD CONSTRAINT ck_notifications_reference_type CHECK (
    reference_type IS NULL OR reference_type IN ('PURCHASE', 'ORDER', 'LOAN', 'CHIT', 'OUTSOURCING_JOB', 'INVENTORY')
);

ALTER TABLE notification_configurations DROP CONSTRAINT ck_notification_configurations_type;
ALTER TABLE notification_configurations ADD CONSTRAINT ck_notification_configurations_type CHECK (
    notification_type IN ('SUPPLIER_DUE', 'CUSTOMER_OUTSTANDING', 'LOAN_DUE', 'CHIT_DUE', 'LOW_STOCK', 'PENDING_OUTSOURCING')
);
```

---

## 4. API Specification & Scheduled Jobs

### 4.1 Scheduled Jobs
1. `LoanDueReminderJob`:
   - Runs daily at `08:15 Asia/Kolkata` (`cron = "${nexora.notifications.loan-due-cron:0 15 8 * * *}"`).
   - Calls `NotificationService.createLoanDueReminders(today)` and `NotificationService.resolveClosedLoans()`.
2. `ChitDueReminderJob`:
   - Runs daily at `08:20 Asia/Kolkata` (`cron = "${nexora.notifications.chit-due-cron:0 20 8 * * *}"`).
   - Calls `NotificationService.createChitDueReminders(today)` and `NotificationService.resolveCompletedChits()`.

### 4.2 API Contract
- `GET /api/v1/notifications`: Supports filtering by `type=LOAN_DUE` and `type=CHIT_DUE`.
- `GET /api/v1/notifications/configurations`: Lists all configured notification types.
- `PUT /api/v1/notifications/configurations/{type}`: Allows updating `enabled`, `daysBeforeDue`, `overdueRepeatDays`, `priority` for `LOAN_DUE` and `CHIT_DUE`.

---

## 5. Desktop Client Integration

1. **Reminder Settings (`ReminderSettingsViewModel.kt`, `SettingsScreen.kt`):**
   - Configures alert parameters for `LOAN_DUE` and `CHIT_DUE` alongside `SUPPLIER_DUE`.
2. **Notifications Screen (`NotificationsScreen.kt`):**
   - Renders notification items for loan and chit payment dues with appropriate chips and clickable links to the loan or chit detail screen.
3. **Strings Localization:**
   - Business titles and labels in `strings.properties`.

---

## 6. Verification & Test Plan

1. **Migration & Schema Tests:**
   - `MigrationIntegrationTest`: V18 to V19 migration succeeds on test databases.
   - `DerivedValuesTest`, `ForeignKeyIndexIntegrationTest`.
2. **Unit & Service Tests:**
   - `LoanDueReminderJobTest`: Verifies loan due reminder generation for DUE_SOON, DUE_TODAY, OVERDUE with deduplication and resolution on payoff.
   - `ChitDueReminderJobTest`: Verifies chit installment reminder generation with monthly cycle tracking, deduplication, and resolution on contribution.
3. **API Tests:**
   - `NotificationApiTest`: Querying notifications by type LOAN_DUE and CHIT_DUE.
4. **Desktop Tests:**
   - `ReminderSettingsViewModelTest`, `AppModuleTest`.
