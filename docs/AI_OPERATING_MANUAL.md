# AI Operating Manual — SmartSilk (Nexora)

**Audience:** any AI assistant (Claude, GPT, Gemini, Copilot, Cursor, a local model) helping the developer
build this project. Read this file **completely** before answering anything. If you are a weaker or older
model: follow it literally, never improvise, and say "I am not sure, let me verify" instead of guessing.

**One-line mission:** help a developer who is *new to Java and Spring* finish a **production, real-money**
inventory + ledger system, and *understand* every piece of it. Correctness beats speed. Learning beats cleverness.

Read order for every session: `CLAUDE.md` → this file → `docs/ROADMAP.md` (current task) →
`docs/DOMAIN_RULES.md` → `docs/CONVENTIONS.md` → `docs/DECISIONS.md` → `docs/specs/00-contracts.md`.
Design transcripts in `smart-ledger-docs/` are background only. When documents disagree:
`DOMAIN_RULES.md` and `DECISIONS.md` win.

---

## 1. Starter prompts (the developer pastes one of these)

**Start of session**
```text
Read CLAUDE.md and docs/AI_OPERATING_MANUAL.md, then docs/ROADMAP.md. Tell me the current task.
Stay in GUIDE mode: goal, why, concepts, exact files/commands, skeletons with TODOs, how to verify.
I will write the code myself. Say "write" and I will tell you to write a specific file.
```
**Write mode:** `write <task id or file>` → produce the code, with a comment on every meaningful line.
**Review mode:** `review` → read my real files and the real compiler/test output; bugs first.
**Stuck mode:** `stuck: <exact error text>` → reproduce, read the stack trace, hypothesise, verify smallest check.
**Reference answer key:** the branch `reference` has one tag per task (`ref/task-<id>`). In review mode, compare
my code with `git diff ref/task-<id>` (or open the files there) and explain the *differences that matter*
(bugs, rule violations) separately from harmless style differences.

## 2. Modes

| Mode | Default? | You produce | You must NOT |
|---|---|---|---|
| **GUIDE** | yes | Task block (see §3): goal, why, concepts, paths, commands, skeletons with TODOs, verification, review questions | Create/edit the developer's files |
| **WRITE** | only on "write", "add it", "fix it", "generate" | Complete files with explanatory comments, then run build + tests and show real output | Skip the tests; add features not in the task; touch other tasks' files |
| **REVIEW** | when the developer says they are done | Findings ranked by severity with *why it matters*, then style notes | Rewrite their code unless asked |
| **STUCK** | when they paste an error | Root cause with evidence, smallest fix, the concept behind it | Shotgun-change several things at once |

If unsure which mode: you are in GUIDE mode.

## 3. The GUIDE block (how every task is presented)

1. **Goal** (1–2 sentences) · 2. **Why now** (business reason + where it sits in the architecture) ·
3. **Concepts** (each new class/annotation/keyword explained from zero, with the ADR number if one exists) ·
4. **Files** (exact paths) · 5. **Commands** (exact, with the directory they run from) ·
6. **Skeleton** with `TODO` markers · 7. **Expected behaviour** table (input → result/error code) ·
8. **Verify** (how to run it, what output proves it works) · 9. **What NOT to do** ·
10. **Review questions** (the developer may skip them) · 11. **Interview line** for important concepts.

The developer is a beginner in Java/Spring: explain every new term the first time it appears. Use examples from
this project. Append lessons to `docs/LEARNING_NOTES.md` after each task.

## 4. Non-negotiable rules and how AIs usually break them

| # | Rule | Typical AI mistake | Check before you answer |
|---|---|---|---|
| R1 | Every stock change = one `material_movement` via `InventoryService`; every money change = one `financial_transaction` via `FinanceService`. No client endpoint writes ledgers. | Updating a `quantity`/`balance` column directly; adding `POST /inventory/movements` | Grep your code for writes to `inventory_balances`/`financial_transactions` outside those services |
| R2 | Derived values (stock, balance, outstanding, payable) are never stored or accepted from the client. Only `inventory_balances` is a projection. | Adding `outstanding_amount` or `status = PAID` columns set by the client | Does any request DTO contain a derived field? |
| R3 | Stock and cash never go negative. Check + update under `PESSIMISTIC_WRITE` in the same transaction. | Read-check-write without a lock; check in the controller | Is the lock taken *before* the check? Is there a concurrency test? |
| R4 | One use case = one `@Transactional` service method. No external calls (HTTP, email, WhatsApp, AI) inside it. | `@Transactional` on controllers; calling an API inside the transaction | Where is the transaction boundary? |
| R5 | Financial/inventory history is immutable. Corrections = reversal/adjustment rows. No `PUT` on amounts, no hard deletes. | `DELETE /payments/{id}`; `UPDATE` of an old movement | Is there any update/delete of a ledger row? |
| R6 | Money `BigDecimal`/`NUMERIC(14,2)`, weight `BigDecimal`/`NUMERIC(12,3)`, `HALF_UP`, compare with `compareTo`. Never `double`/`float`. | `double`, `equals` on BigDecimal, `new BigDecimal(0.1)` | Search for `double`, `float`, `.equals(` on numbers |
| R7 | Historical rates/prices are copied into the record, never recalculated from the current master data. | Joining the *current* rate when computing an old amount | Is the rate a column on the transaction row? |
| R8 | Status changes only through named entity methods and legal transitions (`INVALID_STATE_TRANSITION`). | `setStatus(...)`; status changed in a service | Does the entity have public setters? |
| R9 | Payments allocate oldest-first unless explicit allocations are given (always re-validated). Payment + allocations + ledger transaction are atomic. Payment POSTs are idempotent. | Allocating in the controller; trusting client allocations | Are explicit allocations validated for owner/outstanding/sum? |
| R10 | Vuda warp is trade-only. Chit transactions are cash-only. Loans and transfers are not revenue. | Letting vuda enter production; counting a loan as income | Does the rule have a rejecting test? |

## 5. Stack truths (verified in this repo, Spring Boot 4.1.1 / Java 21 / Gradle 9)

Do **not** write Boot 2/3-era code. Facts confirmed by compiling and running this project:

- `jakarta.*` only. Never `javax.*`.
- Jackson 3 databind is `tools.jackson.*`; annotations stay `com.fasterxml.jackson.annotation.*`.
- Security: a `SecurityFilterChain` bean with the lambda DSL. No `WebSecurityConfigurerAdapter`, no `antMatchers`, no `.and()`.
- Starters are modular: check `Nexora-backend/build.gradle` before adding any dependency; never invent artifact names.
- **Annotation clashes:** `jakarta.persistence.*` and `org.springframework.data.annotation.*` both contain `Id` and `Version`.
  With two wildcard imports the compiler reports ambiguity, and Hibernate only honours the **jakarta** ones. Import them explicitly.
- Auditing: `@CreatedDate`/`@LastModifiedDate` fill **timestamps**; `@CreatedBy`/`@LastModifiedBy` fill **users** from `AuditorAware`.
  Putting `@LastModifiedBy` on an `Instant` is a silent bug.
- `java.time.Clock` is the one to inject. `io.micrometer.core.instrument.Clock` is a different class that exists on the classpath.
- Exceptions: a `@RestControllerAdvice` that has a catch-all `Exception` handler **must** extend `ResponseEntityExceptionHandler`,
  otherwise Spring's own 404/405/400 become 500.
- `HttpStatus.UNPROCESSABLE_CONTENT` is the 422 constant in Spring Framework 7.
- Tests: `AbstractIntegrationTest` (`src/test/java/com/nexora/support`) starts a Testcontainers PostgreSQL 17 (Testcontainers 2.x:
  `org.testcontainers.postgresql.PostgreSQLContainer`, artifacts `testcontainers-postgresql`, `testcontainers-junit-jupiter`)
  and injects the datasource with `@DynamicPropertySource`. Tests need Docker running. Never H2.
- MockMvc: `@AutoConfigureMockMvc` lives in `org.springframework.boot.webmvc.test.autoconfigure`.
- springdoc: use the 3.x line (`springdoc-openapi-starter-webmvc-ui`), docs on only in the `dev` profile.
- Local DB: `docker compose up -d` in `Nexora-backend/` (PostgreSQL 17 on port 5431). `./gradlew bootRun` blocks at ~80% because the
  server is running; that is normal. A stale `bootRun` on port 8080 makes new code look broken: `ss -ltnp | grep 8080`.
- If you are not 100% sure an API exists in this version: say so and verify (docs, the jar, a compile). Never guess.

## 6. Code contract (WRITE mode)

**Before writing:** (1) name the task id and read its spec in `docs/specs/` if it exists; (2) list the rules R1–R10 the code touches;
(3) if the task depends on an open question (`DOMAIN_RULES §8`) check the **assumption** recorded there and say so;
(4) read the existing files you will touch. Do not invent names: reuse `shared/*` helpers (`Money`, `Weight`, `Clock`,
`BusinessException`, `ErrorCode`, `BusinessNumberGenerator`, `PageResponse`, `AuditableEntity`).

**Always:**
- Layering: controller → application service (`@Transactional`) → domain entity → repository. Modules talk only through each
  other's application services; cross-module references are UUIDs, not JPA relations.
- Entities: extend `AuditableEntity`, protected no-arg constructor, static factory, intention-revealing methods, getters only.
- DTOs are `record`s with Jakarta validation; request DTOs hold only client-provided fields; responses never expose entities.
- Constructor injection only. Inject `Clock`; never call `Instant.now()`/`LocalDate.now()` in business code.
- Migrations: new `V<n>__<verb>_<what>.sql` only; NOT NULL, CHECK, UNIQUE, FK **and an index on every FK**; named constraints
  (`pk_`, `fk_`, `uq_`, `ck_`, `ix_`); never edit a merged migration; `ddl-auto=validate` must pass.
- Business errors: throw a `BusinessException` subclass with the right `ErrorCode`. No try/catch for business errors in services.
- Every endpoint has `@PreAuthorize("hasAuthority('<PERMISSION>')")`, validated DTOs, and appears in OpenAPI.
- Comments: a short comment on every meaningful line explaining *what and why* (the developer is learning).
- Prove it: run `./gradlew test` and show the real result. Locking/concurrency/money paths also get a concurrency test, and you
  must *break the lock on purpose once* to show the test fails without it.

**Never:**
`double`/`float` for money or weight · `BigDecimal.equals` · public setters on entities · Lombok `@Data/@Setter` on entities ·
field `@Autowired` · `CascadeType.ALL/REMOVE` toward shared entities · `FetchType.EAGER` · `open-in-view` · H2 · `ddl-auto=update` ·
editing a merged migration · hard deletes of business records · a controller that contains business logic · an external call inside
a transaction · logging passwords/tokens/full account numbers · committing secrets · `catch (Exception e) {}` · `System.out.println`
· `SELECT *` in native queries · loops that run one query per row (N+1) · scope creep (no Kafka, Redis, microservices, CQRS).

## 7. Stop and ask the human (do not decide yourself)

- Any open question in `DOMAIN_RULES §8` that has no recorded answer or assumption.
- A new entity, a new business rule, or a changed rule → it is a **requirement change**: propose a DOMAIN_RULES/DECISIONS update.
- Adding a dependency, changing a merged migration, anything about auth, secrets, backups or deployment targets.
- A test that fails and you do not understand why. Report the real output; do not weaken the test.

## 8. Per-task loop

1. Read `docs/ROADMAP.md` → pick the first unticked task. 2. Read its spec/rules. 3. Present the GUIDE block.
4. Developer writes the code (or says "write"). 5. `./gradlew test` green, new migrations apply on an empty DB.
6. REVIEW against §9. 7. Definition of Done (`CONVENTIONS §11`). 8. Tick the roadmap, update "Current phase", append to
`LEARNING_NOTES.md`, add an ADR if a technology choice was made, add a DOMAIN_RULES change-log row if a rule changed.
9. Suggest a Conventional Commit message (`feat(parties): create party with roles`). Do not commit unless asked.

## 9. Review checklist

Correctness · business-rule violations (R1–R10 + `DOMAIN_RULES §4`) · transaction boundary and locking (is the lock before the
check? lock order consistent?) · layering and module boundaries · validation at the trust boundary · authorization (401 vs 403) ·
migration quality (constraints, FK indexes, naming) · money/weight types and rounding · N+1 queries · error codes match
`DOMAIN_RULES §7` · tests (a rejecting test per rule, a failure path, a concurrency test where required) · logging of secrets ·
unnecessary complexity. Explain *why* each issue matters before suggesting the fix.

## 10. Self-check before you send an answer

- [ ] Did I read the task and the relevant rules, or am I answering from memory?
- [ ] Did I name every rule (R1–R10) my code touches?
- [ ] Did I use only APIs verified for Boot 4 / Hibernate 7 / Spring Security 7 (not Boot 2/3 habits)?
- [ ] Is there a test that fails if the rule is broken?
- [ ] Did I explain each new concept for a beginner?
- [ ] Did I stay inside this task's scope?
If any box is unchecked, fix it or say so honestly.

## 11. Interview-worthy concepts to point out when they appear

ACID and transactions · optimistic vs pessimistic locking · deadlock and lock ordering · idempotency keys · N+1 queries ·
401 vs 403 · JWT vs opaque refresh token and rotation · ledger vs projection · reversal vs update · ProblemDetail (RFC 9457) ·
twelve-factor config and fail-fast · Testcontainers vs H2 · why `@Transactional` belongs on use cases · gap-free numbering.
Give a 2–3 sentence "how to explain this in an interview" line for each.
