# Runbook

Step-by-step procedures for the production server. They follow `docs/10-DEPLOYMENT.md` sections 7-11 and
`docs/09-SECURITY.md` section 9, which give the reasons behind each step.

**Conventions.** The server runs everything from `/opt/nexora` as the deploy user (not root). Before running any
procedure, open a shell there and define the short compose command:

```bash
cd /opt/nexora
alias dc='docker compose -f docker-compose.prod.yml --env-file .env.prod'
```

`<host>` is the value of `APP_DOMAIN` in `.env.prod`. Every procedure ends with an entry in the
[operations log](#operations-log) at the bottom of this file.

## Contents

1. [First server setup](#1-first-server-setup)
2. [Deploy](#2-deploy)
3. [Rollback](#3-rollback)
4. [Restore](#4-restore)
5. [JWT secret rotation](#5-jwt-secret-rotation)
6. [Add a user](#6-add-a-user)
7. [Unlock an account](#7-unlock-an-account)
8. [Alerts](#8-alerts)
9. [Routine maintenance](#9-routine-maintenance)

## 1. First server setup

Once per server. Needs a VPS with Ubuntu LTS, a DNS `A` record for `<host>` pointing at it, and the owner's age key
pair (the public key goes on the server, the private key stays offline with the owner).

1. As root: create the deploy user and give it Docker.
   ```bash
   adduser --disabled-password deploy && usermod -aG docker deploy
   apt install -y docker.io docker-compose-v2 age rclone unattended-upgrades
   ufw allow 22,80,443/tcp && ufw enable
   sed -i 's/^#\?PasswordAuthentication .*/PasswordAuthentication no/' /etc/ssh/sshd_config && systemctl reload ssh
   install -d -o deploy -g deploy /opt/nexora
   ```
2. As `deploy`: copy `deploy/docker-compose.prod.yml`, `deploy/Caddyfile`, `deploy/.env.prod.example`,
   `deploy/postgres/` and `deploy/scripts/` from the repository into `/opt/nexora/` (the `Deploy` workflow copies
   all but the example on every deploy).
3. Create the settings file from the example and fill every value (each line is explained in the example):
   ```bash
   cp .env.prod.example .env.prod && chmod 600 .env.prod
   openssl rand -base64 48   # JWT_SECRET
   openssl rand -hex 24      # POSTGRES_PASSWORD, OWNER_DB_PASSWORD (= FLYWAY_PASSWORD), APP_DB_PASSWORD (= DB_PASSWORD)
   ```
   Set `BOOTSTRAP_OWNER_USERNAME` and `BOOTSTRAP_OWNER_PASSWORD` (12-72 bytes, a letter and a digit) for the first
   start only.
4. Off-site backups: `rclone config` (a remote with a write-only credential for the bucket, named as in
   `RCLONE_REMOTE`), and `AGE_PUBLIC_KEY` in `.env.prod`.
5. Let the server pull images: `docker login ghcr.io -u <github user>` with a read-only (`read:packages`) token.
6. First deploy: `./scripts/deploy.sh <image tag>` (section 2). The app creates the owner account on this start.
7. Remove the two `BOOTSTRAP_OWNER_*` lines from `.env.prod`, then `dc up -d app`.
8. Install the cron entries (`crontab -e` as `deploy`):
   ```cron
   30 21 * * * /opt/nexora/scripts/backup.sh >> /opt/nexora/backups/backup.log 2>&1
   */5 * * * * /opt/nexora/scripts/host-check.sh
   0 22 * * 0  docker system prune -f > /dev/null
   ```
9. External checks: an uptime monitor on `https://<host>/actuator/health` (every minute, alert after 2 failures)
   and a dead-man's switch whose URL goes into `HEALTHCHECK_PING_URL` (alert after 26 hours without a ping).
10. Run a restore drill (section 4.1) the same week.

## 2. Deploy

Normal path: GitHub, workflow **Deploy**, **Run workflow**, `image_tag` = the commit sha shown by the green CI run on
`main` (or a release tag), then the owner approves the run. The workflow copies the deploy files and runs
`deploy.sh` on the server.

By hand on the server (same effect):

1. Check that the image exists: `docker manifest inspect ghcr.io/<owner>/nexora-backend:<tag> > /dev/null && echo ok`.
2. Run `./scripts/deploy.sh <tag>`. It takes a `pre-deploy-<tag>` backup, switches `IMAGE_TAG`, starts the new
   version (database migrations run on start), waits for it to be healthy and checks
   `https://<host>/actuator/health` and that `/api/v1/parties` answers `401`.
3. Exit code 0 means live. Any other exit code means the script has already put the previous version back
   (section 3, first row); read its output.
4. Check by hand: log in from the desktop app, open the dashboard. `tail -3 .deploy-history` shows the tags.

## 3. Rollback

| Situation | Steps |
|---|---|
| New version unhealthy or smoke test failed | Nothing to do: `deploy.sh` starts the previous tag automatically and exits non-zero. Confirm with `dc ps` and `tail -3 .deploy-history`. |
| Bug found after a release, data fine | 1. Find the previous good tag: `tail -5 .deploy-history`. 2. Deploy it (section 2) - the Deploy workflow or `./scripts/deploy.sh <previous tag>`. Migrations never need undoing: each release works with the schema of the next one. |
| A migration damaged data | 1. `dc stop app` (no more writes). 2. Restore the `pre-deploy-<bad tag>` backup with section 4.2, using `--label pre-deploy-<bad tag>`. 3. Deploy the previous tag. 4. Write an incident note in the operations log: what broke, what was restored, which entries made after the deploy must be entered again. |
| Leaked JWT secret | Section 5, including step 6. |

## 4. Restore

### 4.1 Monthly restore drill

On a separate machine (the owner's laptop or a temporary VPS), never on the production server. Needs Docker,
`age`, `rclone` with the off-site **read** credential, and the owner's offline private key file.

1. Get the scripts: a checkout of the repository at the released version.
2. Run:
   ```bash
   OWNER_USERNAME=<owner login> OWNER_PASSWORD=<owner password> ./deploy/scripts/restore-drill.sh \
       --from <remote>:<bucket> --key /media/usb/nexora-backup.key --image ghcr.io/<owner>/nexora-backend:<live tag>
   ```
3. The last line is `drill: PASSED ...` or `drill: FAILED ...`. It checks: health `UP`, the reconciliation queries
   return no rows, ledger counts equal those at backup time, the owner can log in and read the dashboard, total
   time under 2 hours. Everything it created is removed afterwards.
4. Copy the result line into the [drill log](#restore-drill-log). A failed drill, or a last pass older than 31
   days, blocks the next release until fixed.

### 4.2 Disaster restore (server lost or database damaged)

Target: back in service within 2 hours, losing at most the day since the last nightly backup.

1. If the server is lost: set up a new one with section 1, steps 1-5 (same `.env.prod` values from the owner's
   copy; leave out the `BOOTSTRAP_OWNER_*` lines). Point DNS at it.
2. If only the database is damaged: stop the stack and move the bad data aside (kept, never deleted):
   ```bash
   dc down
   docker volume create nexora_postgres_data_damaged_$(date +%Y%m%d)
   docker run --rm -v nexora_postgres_data:/from -v nexora_postgres_data_damaged_$(date +%Y%m%d):/to alpine cp -a /from/. /to/
   docker volume rm nexora_postgres_data
   ```
3. Start an empty database (it creates the `nexora_owner` and `nexora_app` roles on first start):
   ```bash
   dc up -d --wait postgres
   dc exec -T postgres psql -U postgres -d nexora_db -tAc "SELECT rolname FROM pg_roles WHERE rolname = 'nexora_app'"
   # prints nexora_app; if empty, wait a few seconds and repeat the second command
   ```
4. Fetch the newest backup (or the label you need) and check it. On the server use a read credential remote
   (`rclone config`), or copy the files over with `scp` from the drill machine:
   ```bash
   mkdir -p restore && cd restore
   NAME=$(rclone lsf <remote>:<bucket> --include 'nexora-nightly-*.dump.age' | sort | tail -1); BASE=${NAME%.dump.age}
   for s in dump.age documents.tar.age counts sha256; do rclone copyto "<remote>:<bucket>/$BASE.$s" "$BASE.$s"; done
   sha256sum -c "$BASE.sha256"                              # three lines ending in OK
   ```
5. Decrypt (the owner plugs in the key) and restore the database as the schema owner:
   ```bash
   age -d -i /media/usb/nexora-backup.key "$BASE.dump.age" > restore.dump
   (cd .. && dc exec -T postgres pg_restore -U postgres -d nexora_db --no-owner --role=nexora_owner --exit-on-error) < restore.dump
   ```
6. Restore the uploaded documents into the documents volume:
   ```bash
   age -d -i /media/usb/nexora-backup.key "$BASE.documents.tar.age" \
     | (cd .. && dc run --rm -T --no-deps --entrypoint tar app -x -C /var/lib/nexora/documents -f -)
   ```
7. Start the version that was live: `cd .. && ./scripts/deploy.sh <tag from .deploy-history or the owner's notes>`.
8. Check, all required:
   ```bash
   dc exec -T postgres psql -U nexora_owner -d nexora_db -v ON_ERROR_STOP=1 < scripts/reconciliation.sql  # (0 rows) x3
   dc exec -T postgres psql -U nexora_owner -d nexora_db -tAc \
     "select 'financial_transactions=' || count(*) from financial_transactions union all
      select 'material_movements=' || count(*) from material_movements"
   cat "restore/$BASE.counts"                                                              # the same two numbers
   ```
   Then the owner logs in and opens the dashboard.
9. Clean up: `shred -u restore/restore.dump && rm -r restore`. Tell the owner which day's entries must be entered
   again (everything after the backup's time stamp in its name, UTC). Write the operations log entry.

## 5. JWT secret rotation

Every 6 months, and at once if the secret may have leaked. Takes under 5 minutes; users stay logged in (the desktop
app refreshes its token automatically).

1. Generate the new secret: `openssl rand -base64 48`.
2. Edit `.env.prod` and replace the value of `JWT_SECRET` (`nano .env.prod`; the file stays mode 600).
3. Restart the app with it: `dc up -d app`.
4. Wait for `healthy`: `dc ps app`.
5. Check: `curl -fsS https://<host>/actuator/health` prints `{"status":"UP"}`, and a login from the desktop app works.
6. Only after a leak: also sign everyone out, so a stolen refresh token is useless:
   ```bash
   dc exec -T postgres psql -U nexora_owner -d nexora_db -c \
     "UPDATE refresh_tokens SET revoked_at = now(), updated_at = now() WHERE revoked_at IS NULL"
   ```
   Everyone logs in again.
7. Write the operations log entry (date, reason; never the secret).

## 6. Add a user

There is no user-management screen yet (a later release); accounts are added on the server. Roles: `OWNER` (every
permission) and `STAFF` (read-only parties, inventory, orders and production).

1. Agree a starting password with the person: 12-72 characters with at least one letter and one digit, not the
   username. They change it later with the owner's help (same steps 2-4 with an `UPDATE`, below).
2. Open the database console: `dc exec postgres psql -U nexora_owner -d nexora_db`
3. In the console, type the password where nothing is echoed or kept in the shell history:
   ```sql
   \prompt 'Password for the new user: ' pw
   ```
4. Create the account (username in lower case) and give it its role:
   ```sql
   BEGIN;
   INSERT INTO users (username, password_hash, display_name)
   VALUES ('selvi', '{bcrypt}' || crypt(:'pw', gen_salt('bf', 10)), 'Selvi');
   INSERT INTO user_roles (user_id, role_id)
   SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'selvi' AND r.name = 'STAFF';
   COMMIT;
   \q
   ```
5. The person logs in from the desktop app.

To **change a forgotten password**, do steps 2-3, then
`UPDATE users SET password_hash = '{bcrypt}' || crypt(:'pw', gen_salt('bf', 10)), updated_at = now() WHERE username = 'selvi';`.
To **remove access**, `UPDATE users SET is_active = false, updated_at = now() WHERE username = 'selvi';` (accounts
are never deleted: their name is on the records they created).

## 7. Unlock an account

Five wrong passwords in a row lock an account for 15 minutes; it unlocks by itself after that. To unlock at once,
after confirming with the person that the wrong attempts were theirs:

1. See the lock:
   ```bash
   dc exec -T postgres psql -U nexora_owner -d nexora_db -c \
     "SELECT username, failed_login_attempts, locked_until FROM users WHERE username = 'selvi'"
   ```
2. Clear it:
   ```bash
   dc exec -T postgres psql -U nexora_owner -d nexora_db -c \
     "UPDATE users SET failed_login_attempts = 0, locked_until = NULL, updated_at = now() WHERE username = 'selvi'"
   ```
3. The person logs in. If the attempts were not theirs, change the password instead (section 6) and look at the
   alerts for that time (section 8).

## 8. Alerts

| Alert | First steps |
|---|---|
| Site down (external monitor) | `dc ps`; `dc logs --tail 200 app`; `dc up -d`. If the last deploy is under an hour old, roll back (section 3). |
| `app restarted after 3 failed liveness checks` | `dc logs --tail 300 app` around the time; usually the database or memory. |
| `app restarted N times in the last hour` | out of memory: `dc logs app \| grep -i OutOfMemory`; raise `mem_limit` or lower `-XX:MaxRAMPercentage`. |
| Backup missing (dead-man's switch) | `tail -50 backups/backup.log`; run `./scripts/backup.sh` by hand; check disk and `rclone`. |
| Disk above 80 % / 90 % | `docker system prune -f`; `du -sh backups`; enlarge the disk if it keeps growing. |
| 5xx burst | `dc logs --since 10m app \| grep '"level":"ERROR"'`; each line has a `requestId` to follow. |
| 401 flood from one address / `refresh_token_reuse` | Check the address in Caddy's log (`dc logs caddy`); block it with `ufw deny from <ip>`; on token reuse, rotate as in section 5 step 6. |
| Certificate expires within 14 days | `dc logs caddy \| grep -i acme`; check that DNS still points at the server and ports 80/443 are open. |

## 9. Routine maintenance

| When | What |
|---|---|
| Daily (automatic) | nightly backup (cron), host check every 5 minutes |
| Weekly (automatic) | `docker system prune -f` |
| Monthly | restore drill (section 4.1); `sudo apt update && sudo apt upgrade -y && sudo reboot` in a quiet hour (the stack starts again by itself); check `df -h` |
| Every 6 months | JWT secret rotation (section 5) |
| Each release | the checklist in `docs/10-DEPLOYMENT.md` section 13 |

## Restore drill log

| Date | Backup file | Elapsed | Result | By |
|---|---|---|---|---|

## Operations log

| Date | Procedure | Notes | By |
|---|---|---|---|
