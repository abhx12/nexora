# Plan Decisions (read before writing or building anything)

Decided up front so no one has to stop and ask during the build. Anything marked **ASSUMPTION** is a business
default chosen for the MVP; it is isolated, easy to change, and must be confirmed with the owner before go-live.
Ask the human ONLY for a serious doubt (money correctness, data loss, security, an irreversible action).

## 1. Source of truth and precedence
1. `docs/DOMAIN_RULES.md` and `docs/DECISIONS.md` (rules + ADRs) win over everything.
2. `docs/CONVENTIONS.md` (patterns, naming, Definition of Done).
3. This file.
4. The numbered plan docs (`01`..`10`), which expand the above.
5. Design transcripts in `smart-ledger-docs/` are background: when two transcripts disagree, the later step wins
   (Step 8 API > Step 7 DB > Step 4-6). Transcript endpoints that conflict with the rules are rejected:
   `POST /material-movements`, `POST /financial-transactions`, `POST /payment-allocations`, `DELETE /customers/{id}`,
   `/customers` (use `/parties`), `PUT` on amounts.

## 2. Product shape
- Backend API (Spring Boot 4.1.1, Java 21, PostgreSQL 17, Flyway) for ONE business (single tenant), plus a later Android
  client (Kotlin + Compose). The Android app is specified (screens, flows) but built after the backend (Track B).
- "End-to-end tests" = API scenario tests against a real PostgreSQL (Testcontainers), e.g.
  login -> supplier -> purchase -> receipt -> stock -> customer -> order -> payment -> outstanding.
- Money `NUMERIC(14,2)`, weight kg `NUMERIC(12,3)`, HALF_UP, `Clock` Asia/Kolkata, timestamps UTC `timestamptz`.

## 3. Assumptions for the open owner questions (DOMAIN_RULES §8)
| # | Decision for the MVP | Where it is isolated |
|---|---|---|
| Q1 | Supplier payable = **accepted kg x purchase rate** (rejected kg is not paid). A supplier return reduces the payable by returned kg x the purchase's rate (a credit). | `PayableCalculator` in procurement/finance query, one SQL view/query |
| Q2 | A customer obligation starts at order **confirmation**: order items of orders in `CONFIRMED`..`COMPLETED` are payable; `PLACED` and `CANCELLED` are not. Payments before confirmation are held as an unallocated **advance**. An order with allocations cannot be cancelled (409 `INVALID_STATE_TRANSITION`: reverse the payment first). | `OutstandingQuery` |
| Q3 | Overdue **reminders** only: `days_before_due` (default 3, configurable 0-30), on the due date, then every `overdue_repeat_days` (default 1) while overdue. **No penalty amount** is computed in the MVP; overdue days are shown. | notifications module |
| Q4 | **No overdraft**: neither cash nor bank accounts may go negative (`INSUFFICIENT_FUNDS`). | `FinanceService.recordTransaction` |
| Q5 | Keep **paise**: item `amount = round(required_weight_kg x rate_per_kg, 2, HALF_UP)`; totals are sums of rounded item amounts. No whole-rupee rounding in the backend. | `Money` helper |
| Q6 | Chits: contributions (OUT) and payouts (IN) are **amounts entered by the user**, cash only. No bidding/dividend calculation. | chits module |
| Q7 | Direct raw-silk / vuda sale goes through a normal **customer order** (items of type RAW_SILK / VUDA_WARP); delivery creates `DIRECT_SALE` movements. No separate sale entity. | orders module |
| Q8 | Only **OWNER** logs in at go-live. `STAFF` role is seeded (read-only) but there is no user-management endpoint in the MVP (`USER_MANAGE` reserved). | identity module |

## 4. Technical decisions
- Hosting (ADR-023 accepted): Docker image + `docker compose` on a VPS: app + PostgreSQL + Caddy (HTTPS). CI/CD GitHub Actions.
- Coverage threshold: **80% line coverage** on `application` + `domain` packages (JaCoCo), build fails below it.
- Dependency audit: OWASP dependency-check (Gradle plugin) fails the build on CVSS >= 7.
- Auth: ADR-014 (HS256 JWT 15 min, opaque refresh 7 days, rotation + reuse detection, BCrypt, 5 failed logins lock 15 min).
- Error codes: those in DOMAIN_RULES §7 plus `METHOD_NOT_ALLOWED` (405), `UNSUPPORTED_MEDIA_TYPE` (415),
  `DUPLICATE_RESOURCE` (409, unique violation). Add more only with a note in DOMAIN_RULES' change log.
- Business number prefixes (via `BusinessNumberGenerator`, `PREFIX-YYYY-NNNN`):
  `PTY` party, `PUR` purchase, `RCT` material receipt, `RTN` supplier return, `MOV` material movement, `ORD` order,
  `PAY` payment, `BAT` production batch, `JOB` outsourcing job, `EXP` expense, `TRF` transfer, `LON` loan, `CHT` chit.
  Work records and allocations have no business number.
- Permissions: the `Permission` enum in code (USER_MANAGE, PARTY_*, PURCHASE_*, INVENTORY_*, ORDER_*, PRODUCTION_*,
  OUTSOURCING_MANAGE, WORK_RECORD_MANAGE, PAYMENT_*, FINANCE_*, EXPENSE_CREATE, LOAN_MANAGE, CHIT_MANAGE, REPORT_VIEW,
  DOCUMENT_MANAGE). OWNER has all; STAFF read-only (PARTY_VIEW, INVENTORY_VIEW, ORDER_VIEW, PRODUCTION_VIEW).
- Party extras: `default_credit_days` (supplier), `default_rolling_rate_per_hour`, `default_warping_rate_per_kg` (worker).
- Pagination: `page` (0-based), `size` (default 20, max 100), `sort`. Lists return `PageResponse<T>`.
- Dates: business dates `DATE` (`yyyy-MM-dd`), timestamps ISO-8601 UTC strings.

## 5. Requirement and task ID schemes (for traceability)
- Functional: `FR-<MODULE>-<nn>` with MODULE in `AUTH, PARTY, PROC, INV, ORD, PAY, PROD, OUT, WORK, FIN, LOAN, CHIT, RPT, NTF, DOC`.
- Non-functional: `NFR-<AREA>-<nn>` with AREA in `PERF, SEC, REL, OBS, USE, DATA, OPS, TEST`.
- Every requirement has Given/When/Then acceptance criteria and maps to at least one named test.
- Tasks are `T<phase>.<nn>` (e.g. `T2.07`), 30-90 minutes, each listing: goal, files, tests, requirement IDs, checkbox.

## 6. What goes in git and what stays local
- Committed (normal project docs): `01-PRD` .. `10-DEPLOYMENT`, `CONVENTIONS`, `DOMAIN_RULES`, `DECISIONS`, `ROADMAP`, README.
- Local only (never committed, listed in `.git/info/exclude`): `CLAUDE.md`, `AGENTS.md`, `.claude/`, `docs/AI_OPERATING_MANUAL.md`,
  `docs/PROMPT_END_TO_END_PROJECT.md`, `docs/LEARNING_NOTES.md`, `docs/PLAN_DECISIONS.md`, `docs/11-DECISIONS.md`,
  `docs/12-PROGRESS.md`, `docs/13-OPEN-QUESTIONS.md`, `docs/REFERENCE_MAP.md`, `docs/specs/`.
- Committed docs must contain **no mention of AI tools, prompts or assistants** (the product's own future "voice/AI" feature is fine).

## 7. Git style
Plain, human, imperative messages: `Add purchase receipt endpoint`, `Fix stock check order in transfers`.
No task-id prefixes, no tags, no attribution or generated-by lines. One commit per coherent task, tests green at each commit.
Never push. Work on the local `reference` branch in the worktree `../smart_ledger_reference`.

## 8. Autonomy stance
Decisions are made up front (this file). During a build: choose the simplest option consistent with the docs, record it in
`docs/11-DECISIONS.md`, continue. Stop and ask only for: possible money/stock miscalculation with no documented rule,
irreversible data loss, a security-sensitive choice, or a change to a merged migration.

## 9. Reconciliation of the stage-A documents (these override 02/03/04/09/10 where they differ)
| # | Conflict | Decision | Reason |
|---|---|---|---|
| R1 | Lender: `LENDER` party role (02) vs free-text `lender_name` (04) | **`LENDER` becomes a fifth party role**; `loans.party_id` FK to parties (party must hold `LENDER`); `loan_type` stays (`BANK`/`PRIVATE`). V3's `ck_party_roles_role` gains `LENDER` (V3 is unmerged, so it is edited in place). | The exact-schema design links loans to a party; lenders need phone/contact like any party. |
| R2 | Vuda warp location: `RAW_STOCK` (02) vs `FINISHED_STOCK` (04) | **Vuda warp lives in `RAW_STOCK`** (trading stock). `RECEIPT`/`PURCHASE_RETURN`/`DIRECT_SALE` for vuda use `RAW_STOCK`. Location/product CHECK: `RAW_STOCK` = RAW_SILK, VUDA_WARP; `INTERNAL_WIP`/`EXTERNAL_WIP` = SILK_WARP, KORA_WARP; `FINISHED_STOCK` = SILK_WARP, KORA_WARP. `DIRECT_SALE` source is `RAW_STOCK` only. | Matches DOMAIN_RULES §3 (`RECEIPT` -> `RAW_STOCK`) without a rule change. |
| R3 | Raw silk becomes warp | Keep 04's design: `material_movements.to_product_type` (nullable, defaults to `product_type`). `CONSUMPTION`/`OUTSOURCE_ISSUE` move RAW_SILK out of `RAW_STOCK` into WIP as the target warp type. `InventoryService.move` takes an optional `toProductType`. | Needed so WIP balances are per warp type. |
| R4 | Reminder timing: offsets `'-2,0'` (04) vs `daysBeforeDue=3` (02) | **02 wins**: `notification_configurations.days_before_due INT NOT NULL DEFAULT 3 CHECK (0..30)`, reminders also on the due date, then every `overdue_repeat_days` (default 1) while overdue. No penalty amount. | Baseline requirements: "approximately 3-4 days before due date, configurable". |
| R5 | Order path | **02 wins**: `CONFIRMED -> READY` allowed (order filled from stock), `COMPLETED` set explicitly after `DELIVERED`. 04's transition list gets this edge. | Delivery from existing stock needs it. |
| R6 | New error codes | Add to DOMAIN_RULES §7: `METHOD_NOT_ALLOWED` 405, `UNSUPPORTED_MEDIA_TYPE` 415, `DUPLICATE_RESOURCE` 409, `RATE_LIMITED` 429. Lock timeout (5 s) -> 409 `CONCURRENT_MODIFICATION`. Unknown enum value in body/query -> 400 `MALFORMED_REQUEST`. | Needed by 03/09 and the handler. |
| R7 | Stock adjustments | ADR-026: `ADJUSTMENT` needs `INVENTORY_ADJUST` + a reason and can never make stock negative (the `quantity_kg >= 0` CHECK stays). | Real stock can't be negative; DOMAIN_RULES "only exception" line is replaced. |
| R8 | ADR-025..032 written inside 03 §16 | Copied into `docs/DECISIONS.md` (the ADR log stays the single home); 03 links to them. | One source of truth. |
| R9 | Work records and expenses | Both have status `RECORDED`/`REVERSED` (reversal creates the compensating transaction); work-record payment status stays derived. | Rule 5 (immutable history). |

## 10. Client: desktop application (decided by the developer 2026-10-08; overrides every Android/mobile statement)
| Topic | Decision |
|---|---|
| Client | **Desktop application**, Kotlin + **Compose Multiplatform (Desktop/JVM)**. Mobile (Android) is **POST-MVP future scope**; screens and view-models are written so they can later be shared with an Android target. |
| Platforms | **Windows 10/11 (x64)** and **Linux (Ubuntu 22.04+ x64)**. Installers: MSI (Windows) and DEB (Linux) via the Compose Gradle plugin `nativeDistributions` (jpackage, bundled JRE). Minimum window 1280x720, designed for 1366x768 and up; keyboard-first data entry. |
| Server | Backend + PostgreSQL on a **central VPS** (ADR-023, Docker compose + Caddy HTTPS). The desktop app talks to `https://<server>/api/v1`; server URL is set once in Settings. Needs internet; no offline writes in the MVP (show a clear "offline, retry" state). |
| Architecture | Module `nexora-desktop/` (separate Gradle build in the repo root). Layers: Compose UI -> ViewModel (StateFlow) -> Repository -> API client (Ktor client + kotlinx.serialization). DI: Koin (simple, multiplatform). Money/weight as `BigDecimal` (JVM) parsed from JSON strings/numbers without floats. |
| Auth on desktop | Access token in memory only. Refresh token stored in the OS credential store (Windows Credential Manager / Linux Secret Service) via `java-keyring`; fallback: none (re-login). 401 -> refresh once -> retry -> else back to login. Idempotency-Key generated per submit and reused on retry. |
| Updates | Manual: a new installer per release (auto-update POST-MVP). App shows its version and the server's version; refuses to work with an incompatible API major version. |
| Testing | View-model and repository unit tests (JUnit 5 + kotlinx-coroutines-test), API client tests against a mock server (Ktor MockEngine), Compose Desktop UI tests (`compose.uiTest`) for the main flows. |
| CI | Desktop build matrix: `ubuntu-latest` (DEB) and `windows-latest` (MSI). |
| Mobile later | ADR-024 (Android) becomes "Superseded by the desktop decision; Android is future scope". |
