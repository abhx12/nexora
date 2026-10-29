# Reference map (local only)

Which commit implements which task, so a rebuild can compare against it.

| Task | Commit | Message |
|---|---|---|
| T1.01 | c82338e | Add JWT security configuration and consistent API error responses |
| T1.02 | bfacfc1 | Add identity tables and user, role and refresh token entities |
| T1.03 | a151192 | Add login endpoint issuing access and refresh tokens |
| T1.04 | 9e75741 | Test login lockout, including parallel wrong passwords |
| T1.05 | ce98caa | Add refresh token rotation with reuse detection and logout |
| T1.06 | 5cfe368 | Test 401 and 403 problem responses and public endpoints |
| T1.07 | 62d29e5 | Add owner bootstrap, current user endpoint and API version info |
| T1.08 | 7695573 | Add party domain model with roles, phone numbers and status rules |
| T1.09 | 511cac0 | Add endpoints to create and view parties |
| T1.10 | 7edb834 | Add party list with filters, escaped search and capped page size |
| T1.11 | a3f5f68 | Add party update, role assignment and activation endpoints |
| T1.12 | 3a54a91 | Add the party role and status check used by other modules |
| T1.13 | 7dd7f45 | Check permissions before request validation and test every endpoint's security |
| T2.01 | 625e462 | Add inventory tables, movement types and the movement rules |
| T2.02 | 26113b6 | Add the inventory service that locks balances before changing stock |
| T2.03 | d8ddcb9 | Prove the stock lock with parallel tests and time out waiting locks after 5 seconds |
| T2.04 | 9fbdca0 | Add stock summary and movement history endpoints |
| T2.05 | a49ab0c | Add manual wastage, stock adjustment and opening stock endpoints |
| T2.06 | 8ae9f5d | Add a nightly check that every stock balance equals its movements |
| T2.07 | 07e2bde | Add procurement tables and the purchase aggregate with its receiving rules |
| T2.08 | 93d656c | Add endpoints to create, view, list and edit purchases |
| T2.09 | 2a21506 | Add purchase receipts that put the accepted weight into raw stock |
| T2.10 | 4fe8578 | Add supplier returns and purchase cancellation |
| T2.11 | fe4072e | Fail the build when a module reaches into another module's repositories or entities |
| T9.01 | 6c4406f | Add desktop client Gradle project with pinned versions |
| T9.02 | d3a5b8a | Add exact decimal JSON serializer and Indian number formatting |
| T9.03 | 3159f9e | Add API client with ProblemDetail error mapping and user messages |
| T9.04 | ec5c0fc | Add settings file and server setup screen with version check |
| T9.05 | bd4e09f | Add login, OS credential store and single shared token refresh |
| T9.06 | 519f8ef | Add application shell with permission-based menu and shortcuts |
| T9.07 | 3f2f7a3 | Add reusable table, field, picker and dialog components with accessibility checks |
| T9.08 | 36ed8ac | Add owner dashboard screen |
| T9.09 | 31e3245 | Add party list, form and detail screens |
| T9.10 | 52fc543 | Add purchase screens with receive silk and supplier return dialogs |
| T9.11 | 827d821 | Add stock summary, movement history and stock adjustment screens |
| T9.12 | bb097de | Add customer order list, form and detail screens |
| T9.13 | 8d5b0d4 | Add payment list, record payment and payment detail screens |
| T9.14 | b552ecd | Add accounts, transactions, transfer and balance adjustment screens |
| T9.15 | 2c68941 | Add expense, loan and chit screens |
| T9.16 | d82bc03 | Add production batch, outsourcing job and work record screens |
| T9.17 | 02eed84 | Add reports, reminders and document screens |
| T9.18 | a01c614 | Add offline banner, slow-server status and settings screen |
| T9.19 | 1498d7e | Add MSI and DEB installer configuration with app icons |
| T9.20 | d30941b | Add desktop CI workflow building the DEB and the MSI |
| T3.01 | 643d4e9 | Add order tables and the customer order aggregate with its state machine |
| T3.02 | df8e7a6 | Add endpoints to create, view and list customer orders |
| T3.03 | 1f2d908 | Add order edit, confirm, cancel, ready and complete actions |
| T3.04 | 202b325 | Add order delivery that moves every item out of stock in one step |
| T3.05 | 71d0dbd | Add finance tables, accounts and the money ledger service that never lets an account go negative |
| T3.06 | bfd78e2 | Add idempotency keys for retried requests and an audit log of sensitive actions |
| T3.07 | 52d1728 | Add customer payments that settle the oldest open order items first |
| T3.08 | c062edf | Validate chosen payment allocations and apply a customer's advance when an order is confirmed |
| T3.09 | 5bce535 | Add supplier payments and the payables owed to each supplier |
| T3.10 | 65e68ae | Add customer outstanding with ageing and the advance held for the customer |
| T3.11 | af356c8 | Add payment reversal, the payment list and allocation history |
| T3.12 | b9d4905 | Lock obligations during payments and require idempotency keys on stock and money routes |
| T3.13 | 0641a40 | Add opening receivables and payables that payments settle oldest first |
| T3.14 | 5233bb5 | Add an end-to-end test of the buy, sell, deliver and pay loop |
| T4.01 | 7cc21c0 | Add production batch tables and the batch aggregate with its reconciliation rule |
| T4.02 | af3ed0e | Add endpoints to plan, view, list and edit production batches |
| T4.03 | 321bdb8 | Start production batches by consuming raw silk and locking their orders |
| T4.04 | 55f3df9 | Complete production batches into finished stock and wastage, and cancel planned ones |
| T4.05 | ced8c6b | Add outsourcing tables and the job aggregate with its receipt limit |
| T4.06 | 1729534 | Create outsourcing jobs and issue raw silk to manufacturers |
| T4.07 | a064407 | Receive warp from manufacturers, list and cancel jobs, and show material outside |
| T4.08 | a4bd6e6 | Pay manufacturers oldest job first and show what is owed to them |
| T4.09 | 425cd21 | Record worker rolling and warping work at the rate in force |
| T4.10 | 4b80001 | List and reverse work records, pay workers oldest work first, and show weekly payable |
| T4.11 | 678bbb4 | Add end-to-end tests of the production and outsourcing loops |
| T5.01 | a626645 | Transfer money between own accounts without deadlocks |
| T5.02 | c415612 | Record and reverse expenses with personal drawings kept apart |
| T5.03 | 1ba4359 | Adjust and close accounts, show account history and the cash position |
| T5.04 | af24c62 | Record loans, repayments and interest through the money ledger |
| T5.05 | ae67313 | Record chit contributions, payouts and organiser payments in cash |
| T5.06 | 54695e6 | Add a finance scenario, random-sequence invariant checks and an idempotency route guard |
| T6.01 | d4e7838 | Add the owner dashboard summary |
| T6.02 | 23064bc | Add the customer outstanding report and customer statement |
| T6.03 | b503db5 | Add supplier payables ageing and purchase reports |
| T6.04 | bf22907 | Add sales and expense reports |
| T6.05 | dd9d0b3 | Add inventory movement, production and outsourcing reports |
| T6.06 | 0b88183 | Add cash and bank, worker payable, and loans and chits reports |
| T6.07 | c83deaf | Add CSV export for lists and reports and a report consistency check |
| T6.08 | f170166 | Add supplier due-date reminders with configurable timing |
| T6.09 | 2796f1d | Send notifications after commit with retries and list them |
| T6.10 | 3875053 | Add document upload, listing and download |
| T7.01 | 5d70099 | Add the coverage gate and requirement and error-code traceability checks |
| T7.02 | 8bc3478 | Add the dependency vulnerability audit and patch Tomcat and Jackson |
| T7.03 | 8348a97 | Add security headers, body size limits and request rate limiting |
| T7.04 | d4d05dc | Restrict the application database user and check logs and SQL for leaks |
| T7.05 | c1e2f1b | Refuse updates and deletes of ledger and other insert-only rows in the database |
| T7.06 | 0962aca | Add request ids, structured request logs, health probes and metrics on a private port |
| T7.07 | c870e34 | Add static checks for clock use, exact types, retention, derived columns, indexes and migration history |
| T7.08 | 5937d5c | Add the five-year reference workload and performance budgets, and resolve reminders on the payment's connection |
| T7.09 | ef1612e | Answer a lost database connection with a clean error and check error messages stay plain |
| T7.10 | bfe5a4c | Freeze the API contract in openapi.json |
| T8.01 | a6818c8 | Add the production Docker image |
| T8.02 | 96d2be8 | Add the production compose stack with Caddy and HTTPS |
| T8.03 | 3ae347a | Stop a production start with a clear message when required settings are missing |
| T8.04 | c8df8e9 | Extend CI with audit, secret scan, compose check and image publishing, and add the deploy workflow with automatic rollback |
| T8.05 | 6ca95ec | Add encrypted backups of the database and documents and the monthly restore drill |
| T8.06 | b379f29 | Add the host check that alerts and restarts the app |
| T8.07 | a82e627 | Add the go-live import procedure and its reconciliation check |
| T8.08 | 45fe71a | Add the runbook and README, and let backups and deploys run while the app is stopped |
| T8.09 (changelog only) | 4db1bc9 | Add the changelog for the first release |
