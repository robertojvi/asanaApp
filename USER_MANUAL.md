# AccessParks Asana App — User Manual

## What this app does

A web app that mirrors your team's Asana projects into a local database for
fast browsing and reporting, lets you edit task due dates (writes straight
back to Asana), create new projects from templates, and control how often
everything stays in sync — all behind a login with three permission levels.

## Roles at a glance

| Can do | Super User | Admin | User |
|---|:---:|:---:|:---:|
| View projects and tasks | ✅ | ✅ | ✅ |
| View reports | ✅ | ✅ | ✅ |
| Edit task due dates | ✅ | ✅ | ❌ |
| Create new projects | ✅ | ✅ | ❌ |
| Run or schedule syncs | ✅ | ✅ | ❌ |
| Create/edit/disable user accounts | ✅ | ❌ | ❌ |

If you're a **User**, everything below still applies to you except the Sync
and Users pages, which won't appear in your navigation bar at all.

---

## Logging in

Go to the app's web address (`http://localhost:3000` if running locally).
Enter your email and password. If you don't have an account, ask a Super
User to create one for you.

Your session stays active until you log out or the token expires (24 hours).

---

## Projects page (home screen)

This is what you land on after logging in.

### Browsing projects

The left panel lists every project currently tracked, with its completion
percentage next to the name. Click a project (or its checkbox) to select it
— you can select multiple projects at once to view their tasks together.

Use **Select all** / **Clear** at the top to quickly select or deselect
everything.

### Viewing tasks

Once you've selected one or more projects, the task table on the right
shows every task in those projects: name, project, assignee, due date, and
"Expected Due Date" (a custom field some projects use in addition to the
standard due date).

Completed tasks appear faded out but stay visible.

### Editing due dates (Admin / Super User only)

Click into a date field and pick a new date. As soon as you click away from
the field, it saves — **directly to Asana first**, then to the local
database. If the save fails (bad connection, revoked token, etc.), you'll
see an error and the change will **not** be reflected locally, so the app
never shows a date as saved when it isn't.

Tasks whose project doesn't use the "Expected Due Date" custom field show
"n/a" in that column — there's nothing to edit there.

### Creating a new project (Admin / Super User only)

Click **+ New project** in the left panel.

- **Name**: required
- **Duplicate from template**: pick one of the known AccessParks templates
  (Implementation Template, Template Mobile Home, Template Pole, Pre-Sale
  Template, Decommission Template) to copy over the correct custom fields,
  sections, and task structure. This is the recommended option for anything
  matching an existing project type.
- Leave the template dropdown on "-- blank project --" to create an empty
  project with just a name.

Click **Create**. Duplicating from a template can take up to a minute since
Asana processes it as a background job — the app waits for it to finish and
then pulls the new project into the list automatically. You'll see a status
message when it's done.

---

## Reports page

Available to everyone, including read-only Users.

### Progress by Corporate Brand

A rollup table showing, for each brand (Boavida, KOA, Impact, Cobblestone,
QRV, etc.): how many projects, total tasks, completed tasks, and percent
complete. Useful for a high-level "how are we doing" view across the whole
portfolio.

Note: only projects where the "Corporate" field has actually been filled in
appear here — some projects don't have that field set and won't show up
under any brand.

### Overdue Tasks

A list of every task past its due date that isn't yet marked complete,
sorted soonest-overdue first. Capped at the first 100 for readability — if
you need the full list, ask about exporting it or querying the database
directly.

---

## Sync page (Admin / Super User only)

Controls how the local database stays in sync with Asana.

### Run sync now

Click **Run sync now** to immediately pull fresh data from Asana into the
database for every currently tracked project. With ~200 projects this can
take several minutes — the page shows progress status and a final
succeeded/failed count when done.

### Schedule

Set a cron expression (Spring's format: seconds, minutes, hours, day, month,
weekday) for how often syncs run automatically. The default is
`0 0 3 * * *` — 3am every day.

Toggle **Scheduled sync enabled** off if you want to pause automatic syncs
without losing your saved schedule.

Click **Save schedule** to apply changes — takes effect immediately, no
restart needed.

**Don't run this alongside a separate cron job** calling the old Node sync
scripts — pick one method, or you'll needlessly double your Asana API calls
and risk hitting rate limits.

---

## Users page (Super User only)

### Creating a user

Fill in email, password, full name (optional), and role, then click
**Create user**. The new user can log in immediately with those credentials.

### Editing a user

- **Role**: change via the dropdown in their row — takes effect on their
  next login (or immediately if you build in live permission refresh later)
- **Enable/Disable**: toggles whether they can log in at all, without
  deleting their account or history

### Changing your own password

Currently done the same way as editing any other user — find your own row,
and there's a password field available through the edit action. (If this
isn't wired into the current UI yet, ask your Super User or check with
support — password changes may need to go through the API directly for now.)

---

## Glossary

- **gid**: Asana's unique ID for a project, task, or other object (a long
  number). You won't normally need to know these, but they show up in
  behind-the-scenes references.
- **Custom field**: a field Asana projects can define beyond the built-in
  ones (name, assignee, due date) — e.g. "Corporate," "State," "Partial
  Status." Not every project uses the same custom fields.
- **Template project**: a project set up as a starting point to duplicate
  from, rather than to track real work in. AccessParks keeps several of
  these (Implementation Template, Template Pole, etc.).
- **Sync**: the process of pulling current data from Asana into the local
  database, so the app can show fast, rich reports without hitting Asana's
  API on every page load.

---

## Getting help

If something looks wrong (a project missing, a report showing unexpected
numbers, a sync failing repeatedly), the fastest path is usually:

1. Check the **Sync** page for the last run's status
2. Try **Run sync now** to see if a fresh sync resolves it
3. If it persists, note the exact error message and bring it to whoever
   manages the app's backend for troubleshooting
