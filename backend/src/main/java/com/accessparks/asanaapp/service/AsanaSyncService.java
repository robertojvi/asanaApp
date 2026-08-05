package com.accessparks.asanaapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AsanaSyncService {

    private final AsanaClient asanaClient;
    private final JdbcTemplate jdbcTemplate;
    private final FieldNameMapper fieldNameMapper;
    private final SyncProgressTracker progressTracker;

    private static final String TASK_OPT_FIELDS = String.join(",",
        "name", "notes", "completed", "completed_at", "due_on",
        "created_at", "modified_at",
        "assignee.gid", "assignee.name", "assignee.email",
        "memberships.section.gid", "memberships.section.name",
        "custom_fields.gid", "custom_fields.name", "custom_fields.type",
        "custom_fields.display_value", "custom_fields.enum_value.name",
        "custom_fields.multi_enum_values.name",
        "custom_fields.people_value.name", "custom_fields.date_value.date"
    );

    /** Resyncs every project already tracked in the local database (nightly job). */
    public SyncResult syncAllTrackedProjects() {
        List<String> gids = jdbcTemplate.queryForList("SELECT gid FROM projects", String.class);
        if (!progressTracker.tryStart(gids.size())) {
            throw new IllegalStateException("A sync is already running");
        }
        try {
            int succeeded = 0, failed = 0;
            List<String> errors = new ArrayList<>();

            for (String gid : gids) {
                try {
                    syncProject(gid);
                    succeeded++;
                    progressTracker.recordResult(true);
                } catch (Exception e) {
                    failed++;
                    errors.add(gid + ": " + e.getMessage());
                    progressTracker.recordResult(false);
                }
            }
            return new SyncResult(succeeded, failed, errors);
        } finally {
            progressTracker.finish();
        }
    }

    /** Syncs one project (and all its tasks/custom fields) into MySQL. */
    public JsonNode syncProject(String projectGid) {
        JsonNode project = asanaClient.get("/projects/" + projectGid,
            Map.of("opt_fields", "name,archived,modified_at,workspace.gid,workspace.name")).get("data");

        String workspaceGid = project.get("workspace").get("gid").asText();
        String workspaceName = project.get("workspace").get("name").asText();

        upsertWorkspace(workspaceGid, workspaceName);
        upsertProject(project, workspaceGid);

        List<JsonNode> tasks = getAllTaskPages(projectGid);
        for (JsonNode task : tasks) {
            upsertTask(task, projectGid);
        }
        return project;
    }

    private List<JsonNode> getAllTaskPages(String projectGid) {
        List<JsonNode> all = new ArrayList<>();
        String offset = null;
        do {
            Map<String, String> params = new HashMap<>();
            params.put("opt_fields", TASK_OPT_FIELDS);
            params.put("limit", "100");
            if (offset != null) params.put("offset", offset);

            JsonNode page = asanaClient.get("/projects/" + projectGid + "/tasks", params);
            page.get("data").forEach(all::add);

            JsonNode nextPage = page.get("next_page");
            offset = (nextPage != null && !nextPage.isNull()) ? nextPage.get("offset").asText() : null;
        } while (offset != null);
        return all;
    }

    private void upsertWorkspace(String gid, String name) {
        jdbcTemplate.update(
            "INSERT INTO workspaces (gid, name) VALUES (?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name)",
            gid, name
        );
    }

    private void upsertProject(JsonNode p, String workspaceGid) {
        jdbcTemplate.update(
            "INSERT INTO projects (gid, workspace_gid, name, archived, updated_at) VALUES (?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE name=VALUES(name), archived=VALUES(archived), updated_at=VALUES(updated_at)",
            p.get("gid").asText(), workspaceGid, p.get("name").asText(),
            p.get("archived").asBoolean(), toTimestamp(textOrNull(p, "modified_at"))
        );
    }

    private void upsertUser(JsonNode assignee) {
        if (assignee == null || assignee.isNull()) return;
        jdbcTemplate.update(
            "INSERT INTO users (gid, name, email) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE name=VALUES(name), email=VALUES(email)",
            assignee.get("gid").asText(), textOrNull(assignee, "name"), textOrNull(assignee, "email")
        );
    }

    private void upsertSection(JsonNode section, String projectGid) {
        if (section == null || section.isNull()) return;
        jdbcTemplate.update(
            "INSERT INTO sections (gid, project_gid, name) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE name=VALUES(name)",
            section.get("gid").asText(), projectGid, textOrNull(section, "name")
        );
    }

    private void upsertTask(JsonNode t, String projectGid) {
        JsonNode assignee = t.get("assignee");
        upsertUser(assignee);

        JsonNode memberships = t.get("memberships");
        JsonNode section = (memberships != null && memberships.size() > 0) ? memberships.get(0).get("section") : null;
        String sectionGid = null;
        if (section != null && !section.isNull()) {
            upsertSection(section, projectGid);
            sectionGid = section.get("gid").asText();
        }

        jdbcTemplate.update(
            "INSERT INTO tasks (gid, project_gid, section_gid, name, notes, completed, completed_at, assignee_gid, due_on, created_at, modified_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE section_gid=VALUES(section_gid), name=VALUES(name), notes=VALUES(notes), " +
            "completed=VALUES(completed), completed_at=VALUES(completed_at), assignee_gid=VALUES(assignee_gid), " +
            "due_on=VALUES(due_on), modified_at=VALUES(modified_at)",
            t.get("gid").asText(), projectGid, sectionGid, textOrNull(t, "name"), textOrNull(t, "notes"),
            t.get("completed").asBoolean(), toTimestamp(textOrNull(t, "completed_at")),
            assignee != null && !assignee.isNull() ? assignee.get("gid").asText() : null,
            textOrNull(t, "due_on"), toTimestamp(textOrNull(t, "created_at")), toTimestamp(textOrNull(t, "modified_at"))
        );

        JsonNode customFields = t.get("custom_fields");
        if (customFields != null) {
            for (JsonNode cfv : customFields) {
                String canonicalName = fieldNameMapper.canonicalize(cfv.get("name").asText());

                jdbcTemplate.update(
                    "INSERT INTO custom_fields (gid, project_gid, name, canonical_name, type) VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE name=VALUES(name), canonical_name=VALUES(canonical_name), type=VALUES(type)",
                    cfv.get("gid").asText(), projectGid, cfv.get("name").asText(), canonicalName, cfv.get("type").asText()
                );

                jdbcTemplate.update(
                    "INSERT INTO task_custom_field_values (task_gid, custom_field_gid, canonical_name, value_text) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE canonical_name=VALUES(canonical_name), value_text=VALUES(value_text)",
                    t.get("gid").asText(), cfv.get("gid").asText(), canonicalName, displayValue(cfv)
                );
            }
        }
    }

    private String displayValue(JsonNode cfv) {
        String type = cfv.get("type").asText();
        switch (type) {
            case "enum":
                JsonNode enumValue = cfv.get("enum_value");
                return (enumValue != null && !enumValue.isNull()) ? textOrNull(enumValue, "name") : null;
            case "multi_enum":
                List<String> names = new ArrayList<>();
                JsonNode multiValues = cfv.get("multi_enum_values");
                if (multiValues != null) multiValues.forEach(v -> names.add(v.get("name").asText()));
                return String.join(", ", names);
            case "people":
                List<String> people = new ArrayList<>();
                JsonNode peopleValues = cfv.get("people_value");
                if (peopleValues != null) peopleValues.forEach(v -> people.add(v.get("name").asText()));
                return String.join(", ", people);
            case "date":
                JsonNode dateValue = cfv.get("date_value");
                return (dateValue != null && !dateValue.isNull()) ? textOrNull(dateValue, "date") : null;
            default:
                JsonNode display = cfv.get("display_value");
                return (display != null && !display.isNull()) ? display.asText() : null;
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    // Converts Asana's ISO 8601 timestamp ("2026-07-18T12:30:13.033Z") into a
    // java.sql.Timestamp, same fix as the Node version's toMysqlDatetime().
    private static final Pattern ISO_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2}:\\d{2})");
    private Timestamp toTimestamp(String iso) {
        if (iso == null) return null;
        Matcher m = ISO_PATTERN.matcher(iso);
        if (!m.find()) return null;
        LocalDateTime dt = LocalDateTime.parse(m.group(1) + "T" + m.group(2), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return Timestamp.valueOf(dt);
    }

    public record SyncResult(int succeeded, int failed, List<String> errors) {}
}
