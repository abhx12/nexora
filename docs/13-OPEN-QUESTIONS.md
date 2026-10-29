# 13 — Open questions and blockers (local only)

Issues where a check failed 3 times, or a serious doubt was found. Business assumptions Q1-Q8 are tracked in
`PLAN_DECISIONS.md` section 3.

| Date | Task | Issue | Attempts made | Workaround used | Status |
|---|---|---|---|---|---|

## OQ (2026-10-08, T5.04) Payment method of loan payments is not stored
The loan payment request carries `paymentMethod` (checked against the account), but `financial_transactions` has no method column and the
spec says loan payments have no table of their own. The loan response therefore shows `CASH` for cash accounts and `null` for bank
accounts (UPI / transfer / cheque are not distinguished). If the owner needs the method later, add a nullable `payment_method` column to
`financial_transactions` in a new migration. Low priority: no money figure depends on it.

## OQ (2026-10-08, M6 gate) Multipart body above the servlet limit gets Tomcat's HTML 413
A real upload above `spring.servlet.multipart.max-request-size` (12 MB; 13 MB tried on bootRun) is rejected by
`DispatcherServlet.checkMultipart` with Tomcat's raw `SizeLimitExceededException` (not converted to Spring's
`MaxUploadSizeExceededException`), and the `/error` re-dispatch fails on the same body, so the client gets Tomcat's HTML
"413 Payload Too Large" page instead of a problem+json body. Nothing is stored and no server version is shown. Uploads
between 10 and 12 MB get the specified 400 VALIDATION_FAILED. **Planned fix: T7.03** (09-SECURITY section 5: a request-size
filter rejects bodies over 1 MiB, 10 MiB on /documents, by Content-Length before any parsing, with a JSON problem body).
MockMvc does not run the servlet multipart limits, so the test for it must start the real server or call the filter.
**Resolved 2026-10-09 (T7.03):** RequestSizeLimitFilter refuses the declared Content-Length first; bootRun check: a 13 MB
upload now gets the JSON 400 VALIDATION_FAILED body.

## OQ (2026-10-09, T7.02) The dependency-check gate needs an NVD API key
`./gradlew dependencyCheckAnalyze` (plugin 13.0.0) refuses to update its database without an NVD API key ("Invalid API Key,
length of 0"), so the official gate (NFR-SEC-07, M7 gate row 2) cannot run on this machine. The plugin is configured to read
`NVD_API_KEY` from the environment (09-SECURITY section 12; 10-DEPLOYMENT lists it as a CI secret). **Action for the developer:**
request a free key at https://nvd.nist.gov/developers/request-an-api-key, then `export NVD_API_KEY=...` locally and add it as the
GitHub Actions secret `NVD_API_KEY`. Meanwhile the same rule (no High/Critical) was checked with Trivy 0.74.0 on the boot jar: it
found 3 critical Tomcat and 5 high Jackson CVEs in the Boot 4.1.1 BOM versions; patched by overriding `tomcat.version` 11.0.25,
`jackson-bom.version` 3.1.7 and `jackson-2-bom.version` 2.21.7; re-scan: 0 High/Critical. Status: waiting for the key (gate row 2).

## OQ (2026-10-09, T8.09 / M8 gate) Steps that only the developer and the owner can do
The plan is built to the end of M8 except what needs a real server, GitHub, or the owner (a tag and a push are outside what
the reference build may do). In order:
1. NVD API key (OQ above): run `./gradlew dependencyCheckAnalyze` on the release commit; add the `NVD_API_KEY` GitHub secret.
2. Push `reference`/`main`; check the first CI run (build, audit, gitleaks, compose-check, image) is green; set branch
   protection and the `production` environment with the owner as required reviewer, plus `DEPLOY_SSH_KEY`, `DEPLOY_HOST`,
   `DEPLOY_USER`.
3. VPS: `docs/RUNBOOK.md` section 1 (domain, `.env.prod`, rclone write-only remote, the owner's age key pair, cron, external
   uptime monitor and dead-man's switch).
4. M8 gate on the real host: row 1 `curl -fsS https://<host>/actuator/health`, row 2 the restore drill from the real bucket
   (RUNBOOK 4.1), row 3 build + dependency audit on the release commit.
5. Go-live import with the owner (`deploy/import/README.md`) and the signed sign-off sheet; `golive-baseline` backup.
6. Tag `v1.0.0` (`git tag -a v1.0.0 -m "First release" && git push origin v1.0.0`), set the CHANGELOG date, deploy the tag.
7. Parallel run 2-4 weeks with a weekly reconciliation sheet signed by the owner; then tick T8.09.
Everything that could be checked locally was: production stack over HTTPS, deploy with rollback, first-install deploy,
backup + restore drill, disaster restore, host-check alerts, every runbook procedure (388 tests, 0 failures).
