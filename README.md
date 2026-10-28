# SmartSilk (Nexora)

Business system for a family silk-yarn trading and warping business: purchases and receipts, stock and work in
progress, orders and deliveries, production and outsourcing, worker work, payments with allocation, expenses, loans,
chits, accounts, reports and documents. Stock, receivables, payables and cash are always derived from two
append-only ledgers, so every figure can be traced to the entries behind it.

| Part | Folder | Stack |
|---|---|---|
| Server | `Nexora-backend/` | Java 21, Spring Boot 4, PostgreSQL 17, Flyway |
| Desktop app | `nexora-desktop/` | Kotlin, Compose Desktop (Windows and Linux) |
| Production deployment | `deploy/` | Docker Compose, Caddy (HTTPS), scripts for deploy, backup, restore and monitoring |
| Documentation | `docs/` | requirements, architecture, data model, API, testing, security, deployment, runbook |

---

## Download, Install & Uninstall (For End Users — No Source Code Needed)

If you are an end-user, business manager, or tester, **you do not need to clone the source code, install Java, or build the project.** Pre-built standalone application packages are automatically built by CI and available under **GitHub Actions $\rightarrow$ Desktop $\rightarrow$ Artifacts**.

### 1. Download Options
| Platform / Target | Artifact Name | Download Contents | Description |
|:---|:---|:---|:---|
| **Windows All-in-One Installer** | `smartsilk-windows-installer` | `SmartSilk-1.0.0.msi` | Complete standalone Windows app with embedded backend & DB. Installs to Program Files. |
| **Windows Portable** | `smartsilk-windows-portable` | `SmartSilk-Windows-Portable.zip` | Standalone portable application (extract and run directly without install). |
| **Linux All-in-One Package** | `smartsilk-linux-deb` | `smartsilk_1.0.0_amd64.deb` | Complete standalone Debian/Ubuntu package with embedded backend & DB. |
| **Dedicated Server Engine** | `smartsilk-server-engine` | `smartsilk-server/` | Standalone Docker backend & PostgreSQL 17 server for shops/cloud hosting. |

---

### 2. Standalone Server Engine Setup (For Multi-User / Cloud / Shop Host)

If you want to host the SmartSilk business engine on a central machine (e.g. billing counter PC, office server, or cloud VPS) so multiple desktop devices can connect over the local network or internet:

1. Download **`smartsilk-server-engine`** and extract it on the host server.
2. Start the server engine:
   - **On Linux / macOS:** Run `./start-server.sh`
   - **On Windows:** Double-click `start-server.bat`
3. The engine will initialize PostgreSQL 17 and the backend service on port `8088`.
4. On any client PC with SmartSilk installed, open **SmartSilk**, select **Remote / Cloud Server**, enter the server IP (e.g. `http://<SERVER-IP>:8088`), and connect!

---

### 3. Windows Installation & Uninstallation (All-in-One Desktop)

#### Installation:
- **Standard Installer (`.msi`):** Double-click `SmartSilk-1.0.0.msi` and follow the setup wizard. It installs into `Program Files` and creates a Desktop shortcut.
- **Portable Zip (`.zip`):** Extract `SmartSilk-Windows-Portable.zip` to any folder and double-click `SmartSilk.exe`. No installation required!
- **Running Locally:** The built-in local engine and embedded PostgreSQL database start automatically. Zero Docker or external database needed!

#### Uninstallation:
1. Open Windows **Settings** $\rightarrow$ **Apps** $\rightarrow$ **Installed Apps** (or Control Panel $\rightarrow$ Programs and Features).
2. Locate **SmartSilk** in the list.
3. Click the menu (`...`) and select **Uninstall**.

---

### 4. Linux Installation & Uninstallation (All-in-One Desktop)

#### Installation:
Download `smartsilk_1.0.0_amd64.deb` and install via terminal:
```bash
sudo dpkg -i smartsilk_1.0.0_amd64.deb
# If any dependencies are needed:
sudo apt-get install -f
```
The **SmartSilk** application icon will immediately appear in your desktop application launcher / Super Menu.

#### Uninstallation:
To cleanly remove the application from your Linux computer:
```bash
sudo apt remove smartsilk
```
To remove the application and purge any system configurations:
```bash
sudo apt purge smartsilk
```
To remove user cache and saved settings:
```bash
rm -rf ~/.config/smartsilk ~/.local/share/smartsilk
```

#### Why does Linux Settings display "Not sandboxed"?
- Linux desktop environments (such as GNOME / Ubuntu Settings $\rightarrow$ Applications) classify containerized applications (like Flatpak or Snap) as "Sandboxed".
- Native Debian packages (`.deb`)—such as Google Chrome, VS Code, Steam, and SmartSilk—install directly to native system paths (`/opt/smartsilk`) with direct access to native Wayland/X11 rendering, hardware acceleration, and system file dialogs.
- "Not sandboxed" is standard for native Linux software and confirms that the app runs natively with zero container virtualization overhead.

---

---

## Environment Architecture: Developer vs. Customer

SmartSilk maintains strict separation between Developer environments and Customer deployments:

| Aspect | Developer Environment (`Nexora-backend/`) | Customer / Standalone Server (`smartsilk-server/`) |
|:---|:---|:---|
| **Intended User** | Software engineers, contributors, testers | Silk shop owners, weavers, billing counter staff |
| **Server Port** | `8080` (Spring Boot dev runtime) | `8088` (Dedicated containerized engine) |
| **PostgreSQL Port**| `5431` (Container: `postgres-nexora`) | Internal Docker network (Isolated volume) |
| **Environment Config** | `Nexora-backend/.env` (Manual dev overrides) | Auto-generated `.env` with random secrets on first run |
| **Customer Visibility**| Visible to developer (never committed to git) | **Zero-touch**: Customer never edits `.env` directly |
| **Configuration Mode** | CLI / `.env` file | **In-App only**: Managed in Settings screen (`system_settings`) |
| **Default Owner** | `owner` / `DevOwner#2026pass` | Created via Onboarding Wizard or auto-bootstrapped |

---

## Developer Quickstart Guide

### Prerequisites
- **JDK 21** (`java -version`)
- **Docker** and **Docker Compose** plugin (`docker compose version`)
- **Git**

---

### Step 1: Start Local Database (PostgreSQL 17)
From repository root:
```bash
cd Nexora-backend
cp -n .env.example .env    # Creates local dev .env if not present
docker compose up -d        # Starts postgres-nexora on localhost:5431
```
Verify the database is healthy:
```bash
docker exec postgres-nexora pg_isready
```

---

### Step 2: Start the Backend Server
```bash
./gradlew bootRun
```
- **API URL:** `http://localhost:8080`
- **Swagger / OpenAPI Documentation:** `http://localhost:8080/swagger-ui.html`
- **Health Endpoint:** `http://localhost:8080/actuator/health`
- **Default Developer Login:**
  - **Username:** `owner`
  - **Password:** `DevOwner#2026pass`

---

### Step 3: Launch the Desktop Application
In a separate terminal:
```bash
cd nexora-desktop
./gradlew run
```
When prompted for server address, connect to `http://localhost:8080` and log in with the developer credentials above.

---

### Step 4: Seeding & Resetting Data for Development

#### Option A: Seed Rich Sample Business Data
To test orders, stock ledgers, financial reports, supplier dues, and transactions:
```bash
# Run from repository root
docker exec -i postgres-nexora psql -U postgres -d nexora_db < scripts/seed-dev-data.sql
```
*Seeds 10 realistic parties (raw silk suppliers, warp yarn buyers, handloom weavers), opening silk yarn stock balances, 2 active customer orders, and financial accounts.*

#### Option B: Reset to a Completely Clean Database
To test fresh new-user onboarding, empty tutorial checklists, and first-time setups:
```bash
# Run from repository root
docker exec -i postgres-nexora psql -U postgres -d nexora_db < scripts/reset-dev-data.sql
```
*Truncates all ledger transactions, customer orders, and inventory balances while safely preserving the owner account, permissions, and database schema.*

---

## Testing

From `Nexora-backend/` (Docker must be running):

| What | Command |
|---|---|
| Compile with every warning as an error | `./gradlew compileJava compileTestJava` |
| All unit and integration tests | `./gradlew test` |
| End-to-end API scenarios | `./gradlew test --tests '*E2E*'` |
| Coverage report and 80 % gate | `./gradlew jacocoTestReport jacocoTestCoverageVerification` |
| Everything, as CI runs it | `./gradlew build` |
| Performance suite (slow, separate) | `./gradlew test -Pperformance` |
| Dependency audit (needs `NVD_API_KEY`) | `./gradlew dependencyCheckAnalyze` |

From `nexora-desktop/`:
```bash
./gradlew check
```
Lint, type checks and Compose UI tests. Each requirement in `docs/02-REQUIREMENTS.md` is validated by tests matching its ID.

## Deploy

Production runs on one Linux server with Docker: the server image, PostgreSQL and Caddy for HTTPS
(`deploy/docker-compose.prod.yml`).

1. Every push to `main` runs CI (`.github/workflows/ci.yml`): build and tests, dependency audit, secret scan,
   compose check, then the image is scanned, smoke-tested and published to GitHub Container Registry.
2. A release is deployed with the **Deploy** workflow (`.github/workflows/deploy.yml`): enter the image tag, the
   owner approves, and `deploy/scripts/deploy.sh` on the server takes a backup, starts the new version, checks it and
   rolls back by itself if the check fails.
3. Desktop installers: `./gradlew packageDistributionForCurrentOS` in `nexora-desktop/` (MSI on Windows, DEB on
   Linux).

The first server setup, rollback, restore, secret rotation, user accounts and alerts are step by step in
[`docs/RUNBOOK.md`](docs/RUNBOOK.md); the go-live data import is in [`deploy/import/README.md`](deploy/import/README.md);
the reasons behind the deployment design are in [`docs/10-DEPLOYMENT.md`](docs/10-DEPLOYMENT.md).
