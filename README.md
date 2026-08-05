# AccessParks Asana App

Java/Spring Boot backend + React frontend for managing your Asana↔MySQL sync,
browsing/reporting on projects, editing task dates, and creating new projects
from templates — with email/password login and three permission levels.

**Important**: this was built and manually reviewed but **not yet compiled**
(the sandbox it was built in couldn't reach Maven Central to download
dependencies). Run the build steps below on your machine first — if you hit
compile errors, share them and they can be fixed quickly.

## Roles

- **SUPER_USER** — full access, including creating/editing other users
- **ADMIN** — same as Super User except cannot manage users/profiles
- **USER** — read-only: view projects and reports only

## Prerequisites

- Java 17+, Maven, Node.js (already have this from the earlier sync project)
- The existing `asana_mirror` MySQL database (from the earlier Node sync project) — this app reads/writes into the same database

## 1. Backend setup

```bash
cd backend
```

Set required environment variables (or edit `application.yml` directly):
```bash
export DB_USER=root
export DB_PASSWORD=your_mysql_password
export ASANA_TOKEN=your_asana_personal_access_token
export JWT_SECRET=$(openssl rand -base64 48)   # generate a real secret, don't use the default
```

Create the new tables this app needs (on top of your existing schema) and seed the first login:
```bash
mysql -u root -p asana_mirror < src/main/resources/db/init-app-tables.sql
```

This creates a Super User login:
- **Email**: `roberto@accessparks.example` (edit the SQL file to use your real email before running it, or update it after via the Users page)
- **Password**: `ChangeMe123!` — change this immediately after your first login

Build and run:
```bash
mvn spring-boot:run
```
Backend runs on `http://localhost:8080`.

## 2. Frontend setup

```bash
cd frontend
npm install
npm start
```
Opens `http://localhost:3000`, talking to the backend at `localhost:8080` by default (override with `REACT_APP_API_URL` env var if needed).

## 3. Log in

Use the seeded Super User credentials above. From there, go to **Users** to create real accounts for your team with appropriate roles, and change your own password via the Users page (edit your own row).

## What's built

- **Auth**: JWT-based login, 24h token expiry
- **Projects**: list all synced projects with progress stats, view/edit task due dates (writes to Asana first, then mirrors to MySQL), create new projects (blank or duplicated from a template)
- **Reports**: progress by corporate brand, overdue tasks
- **Sync**: manually trigger a full resync, or set a cron schedule (editable at runtime, no redeploy needed)
- **Users**: Super User can create/edit/disable accounts and change roles

## Known limitations / next steps

- No self-service password reset (Super User must reset via the Users page)
- Task editing is limited to `due_on` and the "Expected Due Date" custom field — not other fields like Partial Status
- No pagination on the tasks table — fine for a handful of selected projects, could be slow if you select many large projects at once
- No audit log of who changed what
- CORS is currently hardcoded to `http://localhost:3000` in `SecurityConfig.java` — update this before deploying anywhere else
- `KNOWN_TEMPLATES` list in `ProjectController.java` is hardcoded — consider pulling this dynamically from Asana instead if templates change often
