# AccessParks Asana App — Installation Guide

This covers every component of the system, in the order you need to set them up:

1. MySQL database (`asana_mirror`)
2. Node.js Asana sync scripts (initial data load + optional standing sync)
3. Java Spring Boot backend (the app's API)
4. React frontend (the web app itself)

If you've already completed the MySQL + Node.js steps from earlier setup, skip to **Part 3**.

---

## Prerequisites

Install these if you don't already have them:

| Tool | Check if installed | Install (macOS) |
|---|---|---|
| Node.js 18+ | `node --version` | `brew install node` |
| MySQL 8+ | `mysql --version` | `brew install mysql && brew services start mysql` |
| Java 17+ | `java -version` | `brew install openjdk@17` |
| Maven | `mvn -version` | `brew install maven` |

You'll also need:
- An **Asana Personal Access Token** (Asana → profile → Settings → Apps → Manage Developer Apps → Create New Personal Access Token)
- Your **Asana workspace gid** (`943649575918213` for AccessParks Broadband)

---

## Part 1 — MySQL database

Skip this whole part if `asana_mirror` already exists and has data in it (check with `mysql -u root -p -e "USE asana_mirror; SHOW TABLES;"`).

```bash
cd asanaReporting          # the Node sync project folder
mysql -u root -p < schema.sql
mysql -u root -p < migrations/001_widen_value_text.sql
mysql -u root -p < migrations/002_backfill_canonical_name.sql
mysql -u root -p < migrations/003_project_level_views.sql
```

---

## Part 2 — Node.js sync scripts (initial data load)

Skip this whole part if you've already run a successful `npm run backfill` and your MySQL tables have data.

```bash
cd asanaReporting
npm install
cp .env.example .env
```

Edit `.env`:
```
ASANA_TOKEN=your_personal_access_token
ASANA_WORKSPACE_GID=943649575918213
ASANA_PROJECT_GIDS=<comma-separated list — see project-reference.csv>
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_mysql_password
DB_NAME=asana_mirror
```

Run the initial sync:
```bash
npm run backfill
```

This can take several minutes for 200 projects. Confirm it worked:
```sql
mysql -u root -p asana_mirror -e "SELECT COUNT(*) FROM tasks;"
```

---

## Part 3 — Java Spring Boot backend

```bash
cd asana-app/backend
```

### 3.1 Set environment variables

```bash
export DB_USER=root
export DB_PASSWORD=your_mysql_password
export ASANA_TOKEN=your_asana_personal_access_token
export JWT_SECRET=$(openssl rand -base64 48)
```

Tip: put these in a `.env`-style shell script (e.g. `set-env.sh`) and `source set-env.sh` each time you open a new terminal, so you don't retype them.

### 3.2 Create the app's own tables and seed the first login

Open `src/main/resources/db/init-app-tables.sql` and change the seeded email address to your real one (or leave it and change it after logging in). Then:

```bash
mysql -u root -p asana_mirror < src/main/resources/db/init-app-tables.sql
```

This adds two new tables (`app_users`, `sync_config`) to your existing database and creates one login:
- **Email**: whatever you set in the SQL file
- **Password**: `ChangeMe123!`

### 3.3 Build and run

```bash
mvn spring-boot:run
```

First run downloads dependencies — can take a few minutes. Once it's running, you'll see `Started AsanaAppApplication` in the logs, and the API is live at `http://localhost:8080`.

Quick check it's working:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"your-email@example.com","password":"ChangeMe123!"}'
```
You should get back a JSON response with a `token`.

**If `mvn spring-boot:run` fails to compile**, copy the exact error output — it likely needs a small fix, since this code was written and manually reviewed but not compiled in the environment it was built in.

---

## Part 4 — React frontend

Open a **new terminal** (leave the backend running in the other one):

```bash
cd asana-app/frontend
npm install
npm start
```

This opens `http://localhost:3000` in your browser automatically. It talks to the backend at `localhost:8080` by default.

---

## Part 5 — First login

1. Go to `http://localhost:3000`
2. Log in with the seeded Super User email + `ChangeMe123!`
3. Go to the **Users** page and change your own password immediately (edit your own row, set a new password)
4. Create real accounts for your team with the appropriate roles (see the User Manual for what each role can do)

---

## Verifying everything works — checklist

- [ ] `mysql -u root -p asana_mirror -e "SHOW TABLES;"` shows `app_users` and `sync_config` alongside the original tables
- [ ] Backend logs show `Started AsanaAppApplication` with no errors
- [ ] You can log in at `localhost:3000`
- [ ] The **Projects** page lists your ~200 AccessParks projects with completion percentages
- [ ] Selecting a project shows its tasks with due dates
- [ ] **Reports** page shows the progress-by-corporate table
- [ ] (Super User only) **Users** page lists your account

---

## Ongoing operation

Two ways to keep MySQL synced with Asana going forward — **pick one**, don't run both, or you'll do double the API calls:

**Option A — App-managed schedule (recommended now that the app exists)**
Go to the **Sync** page (Admin/Super User) in the web app and set a cron schedule there. It runs inside the Spring Boot backend as long as it's running.

**Option B — Node cron job (the original approach)**
```bash
crontab -e
```
```
0 3 * * * cd /path/to/asanaReporting && node backfill.js >> sync.log 2>&1
```

If you switch to Option A, remove any cron entry from Option B first.

---

## Troubleshooting quick reference

| Symptom | Likely cause | Fix |
|---|---|---|
| `Table 'asana_mirror.X' doesn't exist` | Schema not loaded | Re-run the `mysql < schema.sql` steps in Part 1 |
| `Incorrect datetime value` during sync | Old code without the ISO-timestamp fix | Make sure you're running the latest `backfill.js`/`AsanaSyncService.java` |
| Login fails with correct password | Seeded hash doesn't match, or wrong email | Re-run `init-app-tables.sql`, double check the email you're using |
| React app shows blank/network error | Backend not running, or wrong port | Confirm `mvn spring-boot:run` is still running in its terminal; check `REACT_APP_API_URL` if backend isn't on 8080 |
| "Failed to update Asana" when editing a date | Asana API rejected the write (bad date format, revoked token, etc.) | Check the backend logs for the detailed Asana error message |
| `mvn spring-boot:run` can't download dependencies | No internet access, or a corporate proxy/firewall blocking Maven Central | Make sure your machine can reach `repo.maven.apache.org` |
