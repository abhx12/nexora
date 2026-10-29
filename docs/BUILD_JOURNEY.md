# SmartSilk build journey

How the application was built, milestone by milestone: the decision problems that came up, the options on the table,
what was chosen and why, and the test that proves the choice holds. `docs/11-DECISIONS.md` is the terse row-per-decision
log; this file tells the story behind it and groups the decisions by the problem they solve.

Each problem is written as **Problem → Options → Decision → Why → Proof**.

---

## 1. Timeline

| When | Stage | What came out of it |
|---|---|---|
| 2026-09-13 .. 09-16 | Business understanding | MVP scope, feature priorities, first architecture notes, empty Spring Boot project |
| 2026-10-02 .. 10-06 | Phase 0 foundation | Package renamed to `com.nexora`, profiles, `AuditableEntity`, `BusinessException`/`ErrorCode`, global error handler, `Clock` bean, `PageResponse`, `Money`/`Weight`, row-locked business numbers |
| 2026-10-07 | Planning reconciliation | `PLAN_DECISIONS.md`: precedence of documents, MVP answers to the eight owner questions (Q1-Q8), nine conflicts between plan documents resolved (R1-R9) |
| 2026-10-07 | M1 identity and parties | JWT login, refresh rotation with reuse detection, lockout, owner bootstrap, parties with roles and phones, endpoint security coverage (106 tests) |
| 2026-10-07 | M2 inventory and procurement | Movement ledger + balance projection with row locks, stock views, adjustments, nightly reconciliation, purchases, receipts, returns, module boundary test (152 tests) |
| 2026-10-08 | Client decision | Developer chose a desktop client (Windows + Linux) over Android; recorded as ADR-035 and PLAN_DECISIONS §10 |
| 2026-10-08 | M9 desktop client | Kotlin + Compose Desktop app: API client, login with OS keyring, every screen, MSI/DEB installers, CI matrix |
| 2026-10-08 | M4 production, outsourcing, workforce | In progress: production batches first |
| 2026-10-07 .. 10-08 | M3 orders and payments | Orders, delivery, money ledger, idempotency, audit log, customer and supplier payments, advance, outstanding, payables, reversal, opening obligations, MVP scenario (227 tests) |

---

## 2. Planning: deciding before building

### 2.1 Documents that disagree
- **Problem:** Ten plan documents and the design transcripts were written at different times and contradict each other
  (lender as a role or a text field, where vuda warp is stored, reminder timing, order path).
- **Options:** Ask the owner about each conflict as it appears; pick per conflict while coding; resolve all of them up front.
- **Decision:** Resolve them up front in one table (PLAN_DECISIONS §9, R1-R9) with a fixed precedence:
  PLAN_DECISIONS §9/§10 > DOMAIN_RULES/DECISIONS > CONVENTIONS > numbered docs > transcripts.
- **Why:** A build that stops for every contradiction never finishes, and a build that decides silently drifts.
  One table makes every resolution visible and reviewable.
- **Examples:** R1 `LENDER` became a fifth party role (loans need contact details); R2 vuda warp lives in `RAW_STOCK`
  (it is trading stock, bought and resold); R5 `CONFIRMED -> READY` is allowed so an order can be filled from stock.

### 2.2 Open business questions with no answer yet
- **Problem:** Eight questions only the owner can answer (when does a customer start owing, is payable on ordered or
  accepted kg, can a bank account go negative, rounding to rupees or paise ...). Phase 3 money code depends on them.
- **Options:** Block the phase until answered; guess inside the code; choose an explicit, isolated assumption.
- **Decision:** An explicit MVP assumption per question (Q1-Q8), each isolated in one place (`OutstandingQuery`,
  `PayableCalculator`, `FinanceService`, ...) and marked "confirm before go-live".
- **Why:** Work continues, and when the owner answers differently only one class changes.
- **Key assumptions:** Q1 payable = accepted kg x rate; Q2 obligation starts at order confirmation, earlier money is an
  advance; Q4 no overdraft; Q5 keep paise, round each item HALF_UP.

### 2.3 Android or desktop
- **Problem:** The plan assumed an Android app; the owner actually works at a shop computer.
- **Options:** Android first; web UI in Electron/Tauri; JavaFX; Compose Multiplatform Desktop.
- **Decision (developer, 2026-10-08):** Compose Desktop for Windows and Linux, Android later (ADR-035).
- **Why:** Same language and UI toolkit as the future mobile app, so view-models carry over; runs on the JVM like the
  backend; keyboard-first data entry suits daily ledger work. Electron would add TypeScript and a browser runtime;
  JavaFX has nothing reusable for mobile.

---

## 3. Security

### 3.1 Clock skew on token expiry
- **Problem:** The security doc mentioned the Nimbus default of 60 s skew, but FR-AUTH-03 requires a token that expired
  one second ago to be rejected.
- **Decision:** Zero skew (`JwtTimestampValidator(Duration.ZERO)`) with the injected `Clock`.
- **Why:** The same server signs and verifies, so there is no clock difference to tolerate.
- **Proof:** a test signs a token that expired 1 s ago and gets 401 `TOKEN_EXPIRED`.

### 3.2 A user without permission got 400 instead of 403
- **Problem:** Spring's method security runs *after* `@Valid`. A read-only STAFF user posting an invalid body got
  400 with the validation rules, not 403, which leaks the rules and breaks the security matrix.
- **Options:** Duplicate every permission as URL rules in the filter chain; accept the 400; evaluate `@PreAuthorize`
  earlier.
- **Decision:** `PermissionCheckInterceptor` evaluates the handler's own `@PreAuthorize` before argument binding.
  `@PreAuthorize` stays the single source of truth; method security still re-checks.
- **Proof:** `EndpointSecurityCoverageTest` walks every handler method and expects 401 without a token and 403 for STAFF.
  With the interceptor removed it fails on `POST /parties` (checked once, then restored).

### 3.3 Login lockout under parallel attempts
- **Problem:** Five wrong passwords lock the account, but five parallel requests can each read "4 failures" and all pass.
- **Decision:** Lock the user row while counting; commit the counter even though the request fails (`noRollbackFor`).
- **Proof:** `LoginLockoutConcurrencyTest`; removing `@Lock` makes it fail with an optimistic-lock error.

### 3.4 Password rule conflict
- **Problem:** FR-AUTH-10 says "at least 10 characters"; the security doc says 12-72 bytes with a letter and a digit.
- **Decision:** The stricter rule, which satisfies both documents.

---

## 4. Concurrency and locking

### 4.1 Stock must never go negative
- **Problem:** Two deliveries of 80 kg and 70 kg from 100 kg can both read "100 available" and both succeed.
- **Options:** Optimistic `@Version` with retries; SERIALIZABLE isolation; pessimistic row lock on the balance.
- **Decision:** `SELECT ... FOR UPDATE` on the `inventory_balances` row before checking (ADR-010), inside one
  `InventoryService.recordMovement` call that every module must use (MANDATORY propagation).
- **Proof:** `InventoryConcurrencyTest` runs 80 and 70 in parallel: exactly one gets 409 `INSUFFICIENT_INVENTORY`.
  Commenting out the lock makes it fail; both restored.

### 4.2 Deadlocks between movements touching two rows
- **Problem:** A movement from A to B and one from B to A, each locking in request order, deadlock.
- **Decision:** One global lock order (ADR-028): idempotency key -> party -> aggregate -> balances (ascending product,
  location) -> accounts (ascending id) -> number sequences.
- **Proof:** 100 opposite two-row movements run in parallel without a deadlock; locking in request order instead
  produced "deadlock detected" (checked once, then restored).

### 4.3 A lock that is never released
- **Problem:** A stuck transaction would make every request behind it hang.
- **Decision:** `lock_timeout = 5s`, `statement_timeout = 30s` sent as a PostgreSQL driver option (not appended to the
  URL, which a deployment may override); a lock timeout maps to 409 `CONCURRENT_MODIFICATION` (ADR-030).
- **Proof:** a test holds a balance lock for longer than 5 s and the second request gets 409.

### 4.4 The document number before the stock lock
- **Problem:** ADR-028 says "numbers last", but a receipt must be inserted (with its `RCT` number) before the movement
  that points at it.
- **Decision:** A documented exception: purchase lock -> `RCT` number -> balance locks -> `MOV` number. No cycle is
  possible because `RCT`/`RTN` are only taken by these use cases, always after the purchase lock.

### 4.5 Order changes racing a payment
- **Problem:** Cancelling or editing an order is only allowed when no payment is allocated to it. A payment allocating
  to the same order between the check and the change would leave money on a cancelled order.
- **Options:** Lock only the order row; rely on `@Version`; lock the customer's party row first, as payments do.
- **Decision:** Every order change locks the customer's party row, then the order row (`PartyService.lockForSettlement`).
  The customer id is read with a projection query so the order is not locked before the party.
- **Why:** Payments lock the party first (ADR-027). Taking the same lock in the same order serializes the two and cannot
  deadlock. `@Version` alone would surface as a vague `CONCURRENT_MODIFICATION`.

### 4.6 Several movements in one use case
- **Problem:** A delivery moves every item of an order. Calling `recordMovement` once per item locks the first stock row,
  takes a `MOV` number (locking the number counter), then waits for the second stock row. Another delivery holding that
  second row and waiting for the counter completes a deadlock.
- **Options:** Sort items before calling per item (still holds the counter between balance locks); a delivery-specific
  inventory method; one generic batch method.
- **Decision:** `InventoryService.recordMovements(list)`: validate everything, lock every touched balance row once in the
  global order, change the balances, then take all `MOV` numbers. `recordMovement` is the one-item case of it.
  Production completion (output + wastage) will reuse it.
- **Proof:** `InventoryConcurrencyTest#oppositeMultiMovementBatches_doNotDeadlock` runs 100 two-item batches in opposite
  item order. With the test switched to item-by-item calls it failed with "deadlock detected"; restored.

### 4.7 A concurrency test that could not fail
- **Problem:** The first parallel-payment test passed even with the party and obligation locks removed.
- **Cause:** Both payments used the same cash account, and its row lock (taken before the obligations are read) also
  serialised them. The test was proving the account lock, not the settlement locks.
- **Decision:** Pay into two different accounts. Removing the locks then allocated 60000 to a 40000 item; restored.
  Payments also lock the party's obligation rows in ascending id order, closing the gap where a supplier return
  (which locks only the purchase) could lower a payable between reading and allocating.
- **Lesson:** a mutation run is what tells you whether a concurrency test proves the lock you think it does.

---

## 5. Data model

### 5.1 Ledger plus projection, not a stock column
- **Problem:** "How much stock do we have" must be fast, and "why is it 50 kg" must be answerable.
- **Decision:** `material_movements` is the append-only truth; `inventory_balances` is a projection updated in the same
  transaction and used as the lock anchor (ADR-009). A nightly job rebuilds the balances from movements with one SQL
  query and logs every difference; it never repairs anything.
- **Why:** A repair would hide the bug that caused the difference; a person must look.

### 5.2 A movement needs the business number of its source
- **Problem:** Movement responses show the source document number (`RCT-2026-0001`). Looking it up would make inventory
  call procurement and orders, which already call inventory: a cycle.
- **Decision:** `material_movements.reference_number` is copied at insert.

### 5.3 Insert cycle between a return and its movement
- **Problem:** The return row needed the movement id and the movement needed the return id. Hibernate flushes before the
  inventory's native insert, so the return was written with a NULL movement id.
- **Decision:** No `material_movement_id` column; the movement points at the return through `reference_type/reference_id`,
  exactly like receipts.

### 5.4 Replacing order lines hit the unique line number
- **Problem:** Replacing the item list deletes old rows and inserts new ones, but Hibernate flushes inserts before deletes,
  so line 1 existed twice.
- **Decision:** Lines are edited in place (same id, same line number), extra lines appended, surplus removed.
  Phones on a party had the same issue and became an `@ElementCollection`, which deletes before inserting.

### 5.5 Payment fields before payments exist
- **Problem:** Order and purchase responses need `allocatedAmount`, `outstandingAmount`, `paymentStatus`, but allocations
  arrive in a later task. Returning constant zeros would be placeholder code in a money path.
- **Decision:** Purchases leave the allocation fields out until allocations exist. Orders carry them from the start
  because the desktop client needs them non-null, computed by one rule (`PaymentStatus` in `shared.domain`) over an
  allocation sum that is honestly empty until payments are recorded.

### 5.6 "Money never goes negative" in the database too
- **Problem:** FR-FIN-06 wants a database-level safeguard behind the service check, but there is no balance column to put a
  CHECK on (balances are derived, rule 2), and the data model defined nothing.
- **Options:** No safeguard; a stored balance column with `CHECK >= 0` (a second derived value to keep in step); a trigger.
- **Decision:** `trg_financial_transactions_not_negative`: after inserting an `OUT` row, sum the account and refuse the row
  if it is below zero. The service still checks first under the account row lock; the trigger only catches a code path
  that forgot to.
- **Proof:** a direct SQL insert past the balance is refused. With the account lock removed, the parallel cash-out test
  fails because the loser is stopped by the trigger instead of getting `INSUFFICIENT_FUNDS`, so money still never went
  negative. That's what the safety net is for.

### 5.7 The ledger's type table, enforced twice
- **Problem:** 17 transaction types, each with a fixed direction and exactly one kind of business reference.
- **Decision:** `TransactionType` carries its direction and reference kind; `FinanceService` refuses a malformed command,
  and two CHECKs (`_type_direction`, `_type_reference`) refuse it in the database. A test walks every type and proves
  both layers accept the right shape and reject the wrong one.

### 5.8 Idempotent POSTs: where the transaction lives
- **Problem:** A retried payment must not be recorded twice. The key, the use case and the stored response must commit
  together, a failed request must leave no key, and a concurrent duplicate must wait and then replay.
- **Options:** A `HandlerInterceptor` (cannot wrap the handler call in a transaction); an aspect around controller
  methods (cannot return a stored JSON body from a method typed `OrderResponse`); a servlet filter.
- **Decision:** `IdempotencyFilter` after Spring Security opens the transaction, claims the key with
  `INSERT ... ON CONFLICT DO NOTHING`, runs the controller (the service's `@Transactional` joins), stores status, body and
  `Location` for a 2xx and rolls back otherwise. PostgreSQL's unique index gives "wait for the first, then replay" for free.
  A missing key is refused by an interceptor placed after the permission check, so STAFF still sees 403 first.
- **Snag:** the filter at first silently did nothing. Two `RequestMappingHandlerMapping` beans exist (Actuator adds one),
  and outside the `DispatcherServlet` the request path must be parsed before a handler lookup. A temporary debug line
  showed `NoUniqueBeanDefinitionException`; the fix was a qualifier and `ServletRequestPathUtils.parseAndCache`.
- **Proof:** replay returns the identical body with `Idempotency-Replayed: true`; same key, other body is 409
  `DUPLICATE_REQUEST`; a request that fails stores no key; five parallel duplicates create exactly one row.

### 5.9 Audit entries that cannot lie
- **Decision:** `AuditService.record` is `MANDATORY`: the entry commits or rolls back with the action it describes. The
  actor is the logged-in user, or empty for system actions (login lock, token reuse). Details are JSON text cast to
  `jsonb` in SQL, so no JSON type library is needed for Hibernate.

### 5.10 Who owns "outstanding"
- **Problem:** Finance must know every customer's open order items to allocate a payment; orders must show each item's
  allocated amount. Two Java services calling each other is a cycle.
- **Options:** Finance calls an orders query service (cycle once orders calls finance for advances); duplicate the logic;
  read-only SQL in finance.
- **Decision:** Finance reads obligations with one SQL CTE over the order and opening-obligation tables (reading is
  allowed; rule 4 only forbids writing another module's tables). Orders asks finance's `AllocationQueryService` for the
  allocated sums. The dependency runs one way: orders -> finance.
- **Rule kept:** outstanding is never stored; it is always amount minus allocations of payments still `RECORDED`, so a
  reversal releases them automatically.

### 5.11 Payment lock order
- **Decision:** idempotency key (filter) -> customer party row -> account row -> `PAY` number -> payment, allocations,
  ledger row. The open obligations are read while the party is locked, so two payments of one customer cannot allocate
  the same rupee. The account is locked before the number (ADR-028 "numbers last"); `FinanceService` re-locks the same
  row without waiting.

### 5.12 One place for the supplier payable formula
- **Problem:** Assumption Q1 (payable = accepted minus returned kg times the rate) is needed by finance (to allocate a
  supplier payment) and by procurement (payables screens, list filters). Copying the SQL into both means an owner answer
  changes two places.
- **Decision:** a database view `purchase_payables` holds the formula once; both modules read it. Supplier advances are
  post-MVP, so an `OUT` payment must land fully on obligations or is refused with `PAYMENT_ALLOCATION_EXCEEDED`.

---

## 6. API semantics

### 6.1 PATCH with Java records
- **Problem:** A record cannot tell "field absent" from "field null", so PATCH cannot clear a field the usual way.
- **Options:** JSON Merge Patch or `JsonNullable` (new dependency); a convention.
- **Decision:** Null keeps the value, an empty string clears an optional text, a list when sent is the full new list.
  The same rule is used for parties, purchases and orders.

### 6.2 History fields in a PATCH body
- **Problem:** Purchase rate, weight and supplier must never change, but unknown JSON fields are ignored elsewhere.
- **Decision:** `UpdatePurchaseRequest` lists those fields typed `Object` with `@Null`, so sending one is 400 naming it.
  Orders simply do not have status/amount fields in the request, so a client sending them changes nothing (proven by a test).

### 6.3 409 or 422
- **Rule used everywhere:** 409 when the current state forbids it (could succeed later), 422 when the request is
  semantically wrong whatever the state (wrong party role, vuda into production). A locked order is 409 `ORDER_LOCKED`;
  cancelling an order with payments is 409 `INVALID_STATE_TRANSITION` ("reverse the payment first").

### 6.4 Sort parameters
- **Problem:** Spring Data accepts any entity property in `sort`, including internal ones like `version`.
- **Decision:** An allow-list per endpoint in `shared.api.ListParameters`; anything else is 400 naming `sort`.

---

## 7. Testing strategy

### 7.1 Real database, never H2
- Every integration test runs against PostgreSQL 17 in Testcontainers (ADR-015): locking, `NUMERIC` and CHECK
  constraints behave differently in H2.

### 7.2 The developer's own database
- **Problem:** The task said `docker compose down -v` to reset the local database, but that container and volume belong to
  the developer's own checkout.
- **Decision:** Never wipe it. Migrations are proven on Testcontainers; manual runs use a separate database `nexora_ref`
  in the same server. Port 8080 was held by another project's container, so smoke runs used port 8090 instead of
  stopping someone else's container.
- **Why:** Irreversible data loss outside this worktree is never acceptable for convenience.

### 7.3 Proving a lock actually matters
- Every lock got a parallel test, and each test was run once with the lock removed to see it fail
  (login lockout, stock balance, two-row deadlock, number sequence). A concurrency test that passes without the lock
  proves nothing.

### 7.4 Module boundaries
- **Problem:** ADR-004 rules (no other module's repositories or entities) were kept by discipline only.
- **Options:** Spring Modulith `verify()` (new dependency, Boot 4.1 support uncertain); a source-scan test.
- **Decision:** `ModuleBoundaryTest`, a plain JUnit source scan plus reflection for `@Entity`. Enums, records and
  exceptions of other modules are allowed because the documented contracts use them.
- **Proof:** an unused `PartyRepository` import in `PurchaseService` made the test fail; removed.

---

## 8. Desktop client

| Problem | Decision | Why |
|---|---|---|
| Lint tool not pinned in the plan | Kotlin `allWarningsAsErrors`; `./gradlew check` = compile + warnings + tests | No new plugin needed; the compiler already flags unused and deprecated code |
| UI tests need a display | Compose `runComposeUiTest` headless; CI still wraps in `xvfb-run` | Verified locally with `DISPLAY` unset |
| Ktor's bearer plugin caches tokens and follows `WWW-Authenticate` | Hand-written refresh step: one refresh shared through a `Mutex`, one retry | Exact "refresh once, retry once, else session-expired dialog" |
| Server compatibility | Compare the API major from `/actuator/info` (`app.api-version`), not the build version | A dev build version would block every login |
| Money in JSON | `BigDecimal` from the JSON literal text, never through `Double` | A float round-trip changes paise |
| kotlinx.serialization drops defaults | `encodeDefaults = true`, `explicitNulls = false` | A required `paymentMethod = CASH` silently vanished; caught by a view-model test |
| A "returnable kg" figure on the return dialog | Show the server's accepted and returned figures, no client subtraction | The client never computes business values (NFR-USE-02) |
| Thirteen reports with different shapes | One generic viewer driven by field-name suffixes | Every figure displayed exactly as sent |
| Test doubles | Real repositories against Ktor `MockEngine` with payloads from the API spec | No interfaces just for tests; the JSON contract is exercised everywhere |

---

## 9. Running log

New entries are added as each task finishes.

| Date | Task | Problem | Decision |
|---|---|---|---|
| 2026-10-08 | T3.03 order edit and status actions | An edit/cancel checks "no allocations" and must not race a payment | Lock customer party row, then order row, same order as payments (see 4.5) |
| 2026-10-08 | T3.04 deliver an order | Per-item stock calls deadlock; delivery must be all or nothing | One batch movement call with all locks first (see 4.6); delivery locks only the order row |
| 2026-10-08 | T3.05 finance schema and ledger | No DB safeguard for negative money; 17-type table | Not-negative trigger (5.6); type table in enum + CHECKs (5.7); GET /transactions/{id} so edits are 405 |
| 2026-10-08 | T3.06 idempotency keys and audit log | Transaction must wrap the whole handler; filter silently skipped | Filter-owned transaction with ON CONFLICT claim (5.8); MANDATORY audit service (5.9) |
| 2026-10-08 | T3.07 customer payment, oldest first | Finance <-> orders cycle; lock order with a number in the middle | SQL obligations in finance (5.10); party -> account -> number (5.11); date defaults to today |
| 2026-10-08 | T3.08 explicit allocations, advance on confirm | Advance must pay a newly owed order without moving money twice | Allocation rows only, oldest payment first, inside confirm's party lock; no ledger row |

| 2026-10-08 | T3.09 supplier payments and payables | Q1 formula needed by two modules; derived list filters | purchase_payables view (5.12); ids from one aggregate query for paymentStatus/overdue filters |
| 2026-10-08 | T3.10 customer outstanding | Advance netted or shown? | Shown beside the total (money held for the customer stays visible); ageing by order date, no customer due dates in the MVP |
| 2026-10-08 | T3.11 payment reversal | Undo a payment without editing history | Status REVERSED + mirror REVERSAL row; allocations untouched and released by the 'RECORDED only' rule; party -> payment -> account locks |
| 2026-10-08 | T3.12 payment concurrency and idempotent routes | Concurrency test passed without the locks under test | Two accounts in the test (4.7); obligation rows locked after the party; keys on receipts, returns, deliver |
| 2026-10-08 | T3.13 opening obligations | Paper-ledger dues at go-live must settle like orders without being revenue | An obligation row only (no ledger, no stock), oldest-first with orders/purchases; FINANCE_MANAGE per the API spec |
| 2026-10-08 | T3.14 MVP scenario + M3 gate | Gate row 4 expects a failure the design prevents | Reported honestly: the party lock alone serialises payments; both settlement locks removed -> fails |
| 2026-10-08 | T4.01 production schema and batch | Batch links replaced while planned; PATCH vs 405 for finished batches | Element collection (delete before insert); 409 for a PATCH that is not PLANNED, 405 only for PUT/DELETE |
| 2026-10-08 | T4.02 batches create/list/edit | Order detail must list its batches, but production depends on orders | Orders reads the links by its own read-only SQL; production uses OrderService.itemRefs |
