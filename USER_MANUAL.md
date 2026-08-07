# AccessParks Asana App — User Manual

## What this app does

A web app that mirrors your team's **Asana** projects, **Jira Cloud**
projects/issues, and the internal **site inventory dashboard** (venues,
locations, and network devices) into a local database for fast browsing and
reporting. It lets you edit Asana task due dates (writes straight back to
Asana), create new Asana projects from templates, see each site's linked
Asana project so you can create/update tasks for it, and control how often
Asana data stays in sync — all behind a login with three permission levels.

## Roles at a glance

| Can do | Super User | Admin | User |
|---|:---:|:---:|:---:|
| View Asana projects and tasks | ✅ | ✅ | ✅ |
| View Jira projects and issues | ✅ | ✅ | ✅ |
| View sites, locations, and devices | ✅ | ✅ | ✅ |
| View reports | ✅ | ✅ | ✅ |
| Edit Asana task due dates | ✅ | ✅ | ❌ |
| Create new Asana projects | ✅ | ✅ | ❌ |
| Run or schedule the Asana sync | ✅ | ✅ | ❌ |
| Run the Jira sync | ✅ | ✅ | ❌ |
| Run the Sites sync | ✅ | ✅ | ❌ |
| Create/edit/disable user accounts | ✅ | ❌ | ❌ |

If you're a **User**, everything below still applies to you except the Sync
and Users pages (which won't appear in your navigation bar at all) and the
**Sync Now** buttons on the Jira Projects and Sites pages, which won't be
visible to you either — you can still view everything on those pages, just
not trigger a refresh.

---

## Logging in

Go to the app's web address (`http://localhost:3000` if running locally).
Enter your email and password. If you don't have an account, ask a Super
User to create one for you.

Your session stays active until you log out or the token expires (24 hours).

---

## Asana Projects page (home screen)

This is what you land on after logging in. (Labeled **Asana Projects** in
the nav bar, to tell it apart from the separate Jira Projects page.)

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

## Jira Projects page

Mirrors your team's **Jira Cloud** projects and issues, the same way the
Asana Projects page mirrors Asana.

### Browsing projects and issues

The left panel lists every Jira project currently tracked. Search narrows
the list as you type. Click a project to select it — you can select
multiple projects at once, same as the Asana Projects page.

Once selected, the table on the right shows every issue in those projects:
key, summary, status, type, priority, assignee, and due date. This view is
read-only — editing Jira issues from this app isn't supported yet.

### Syncing (Admin / Super User only)

Click **Sync Now** in the left panel to pull fresh projects and issues from
Jira Cloud. Unlike the Asana sync, there's no scheduled/automatic option
yet — you have to click it each time you want fresher data. The status
message shows how many projects and issues were synced, and lists any
per-project errors inline if something failed partway through.

If you see "No Jira projects yet," nobody has run a sync since this feature
was set up — click **Sync Now** to pull the first batch.

---

## Sites page

Mirrors the internal site inventory dashboard (venue/subvenue details,
network locations, and devices) into the app, including each site's linked
**Asana project** — useful when you need to jump straight from a site to
its Asana project to create or update tasks and subtasks.

### Browsing sites

The left panel lists every site, sorted alphabetically by venue and site
name. Search narrows the list as you type. A green dot next to a site means
it has a linked Asana project. Click a site to view its details — this page
only shows one site at a time (unlike the multi-select Asana/Jira pages).

### Viewing a site's details

Selecting a site shows:
- A summary card: **Asana Project** (a direct link, when one exists),
  address, website, front desk phone, operating season, office hours,
  managers, ISP, electric utility, and launch status
- **Jira Location/Device List**: every physical location at the site
  (building, pole, etc. with GPS coordinates and notes), and under each
  location, every network device installed there — name, manufacturer,
  model, management IP/MAC, serial number, power source, what it connects
  to upstream, and notes

This is read-only, same as the Jira Projects page.

### Syncing (Admin / Super User only)

Click **Sync Now** in the left panel to re-pull everything from the
internal dashboard. This is a much bigger sync than Jira or Asana — it
fetches roughly 243 sites, ~17,000 locations, and ~31,000 devices, and
takes **around 10-12 minutes**. The page shows a status message the whole
time; it's normal for it to say "Syncing..." for several minutes with
nothing else happening on screen. Like the Jira sync, there's no scheduled
option yet, and it's safe to re-run anytime — it updates existing rows
rather than duplicating them.

If a site's "Contractors" or other free-text fields look garbled or
incomplete, that's a known limitation of scraping the dashboard's page
layout for those specific free-form sections — the well-structured fields
(address, contacts, locations, devices) aren't affected.

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

Controls how the local database stays in sync with **Asana** specifically —
this page doesn't touch Jira or Sites data. Those two have their own
**Sync Now** buttons directly on the Jira Projects and Sites pages instead,
with no scheduling option yet (see those sections above).

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
- **Sync**: the process of pulling current data from Asana, Jira, or the
  site inventory dashboard into the local database, so the app can show
  fast, rich reports without hitting those systems on every page load.
- **Jira issue**: Jira's equivalent of an Asana task — a ticket with a
  status, assignee, priority, and due date, belonging to a Jira project.
- **Site / subvenue**: one physical location AccessParks provides service
  to (e.g. a specific KOA campground), as tracked in the internal site
  inventory dashboard. Each site can belong to a broader "venue" (e.g. a
  KOA property with a marina site and a trailside site) and can have a
  linked Asana project for tracking its implementation/support work.
- **Location** (on the Sites page): a physical spot within a site where
  network equipment lives — a building, pole, etc. — with GPS coordinates.
- **Device** (on the Sites page): a piece of network hardware (router,
  switch, access point, etc.) installed at a location, including its
  management IP/MAC, serial number, and what it connects to upstream.

---

## Getting help

If something looks wrong (a project missing, a report showing unexpected
numbers, a sync failing repeatedly), the fastest path is usually:

1. Check the **Sync** page for the last run's status
2. Try **Run sync now** to see if a fresh sync resolves it
3. If it persists, note the exact error message and bring it to whoever
   manages the app's backend for troubleshooting
