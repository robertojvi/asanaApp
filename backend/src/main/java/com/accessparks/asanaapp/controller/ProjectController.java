package com.accessparks.asanaapp.controller;

import com.accessparks.asanaapp.dto.ProjectDtos.*;
import com.accessparks.asanaapp.service.AsanaClient;
import com.accessparks.asanaapp.service.AsanaSyncService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectController {

    private final JdbcTemplate jdbcTemplate;
    private final AsanaClient asanaClient;
    private final AsanaSyncService asanaSyncService;

    @Value("${asana.workspace-gid}")
    private String defaultWorkspaceGid;

    // Known template projects, excluded from the main tracked list but kept
    // as duplication sources. Update this list as templates change.
    private static final List<Map<String, String>> KNOWN_TEMPLATES = List.of(
        Map.of("gid", "1201923985561000", "name", "Implementation Template"),
        Map.of("gid", "1206526495244277", "name", "Template"),
        Map.of("gid", "1207361024588231", "name", "Template Mobile Home"),
        Map.of("gid", "1207425837094942", "name", "Template Pole"),
        Map.of("gid", "1208641448613509", "name", "Pre-Sale Template"),
        Map.of("gid", "1215635857748827", "name", "Decommission Template")
    );

    @GetMapping("/templates")
    public List<Map<String, String>> listTemplates() {
        return KNOWN_TEMPLATES;
    }

    @GetMapping("/projects")
    public List<Map<String, Object>> listProjects() {
        return jdbcTemplate.queryForList(
            "SELECT project_gid, project, corporate, state, city, total_tasks, " +
            "completed_tasks, open_tasks, pct_complete FROM v_project_overview ORDER BY project"
        );
    }

    @GetMapping("/tasks")
    public List<Map<String, Object>> listTasks(@RequestParam("project_gids") String projectGidsCsv) {
        List<String> gids = List.of(projectGidsCsv.split(","));
        if (gids.isEmpty()) return List.of();

        String placeholders = String.join(",", gids.stream().map(g -> "?").toList());
        String sql = """
            SELECT t.gid AS task_gid, t.name AS task_name, p.name AS project, t.project_gid,
                   t.completed, t.due_on, u.name AS assignee,
                   MAX(CASE WHEN cf.canonical_name = 'expected_due_date' THEN cf.gid END) AS expected_due_date_field_gid,
                   MAX(CASE WHEN cfv.canonical_name = 'expected_due_date' THEN cfv.value_text END) AS expected_due_date
            FROM tasks t
            JOIN projects p ON p.gid = t.project_gid
            LEFT JOIN users u ON u.gid = t.assignee_gid
            LEFT JOIN task_custom_field_values cfv ON cfv.task_gid = t.gid
            LEFT JOIN custom_fields cf ON cf.gid = cfv.custom_field_gid
            WHERE t.project_gid IN (%s)
            GROUP BY t.gid, t.name, p.name, t.project_gid, t.completed, t.due_on, u.name
            ORDER BY p.name, t.due_on IS NULL, t.due_on
            """.formatted(placeholders);

        return jdbcTemplate.queryForList(sql, gids.toArray());
    }

    @PreAuthorize("hasAnyRole('SUPER_USER', 'ADMIN')")
    @PostMapping("/projects")
    public ResponseEntity<?> createBlankProject(@Valid @RequestBody CreateBlankProjectRequest request) {
        try {
            JsonNode created = asanaClient.post("/projects", Map.of("name", request.name(), "workspace", defaultWorkspaceGid)).get("data");
            asanaSyncService.syncProject(created.get("gid").asText());
            return ResponseEntity.ok(Map.of("success", true, "projectGid", created.get("gid").asText()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('SUPER_USER', 'ADMIN')")
    @PostMapping("/projects/duplicate")
    public ResponseEntity<?> duplicateProject(@Valid @RequestBody DuplicateProjectRequest request) {
        try {
            JsonNode job = asanaClient.duplicateProject(request.templateGid(), request.name());
            String newProjectGid = asanaClient.pollJobForNewProjectGid(job.get("gid").asText(), 120_000);
            asanaSyncService.syncProject(newProjectGid);
            return ResponseEntity.ok(Map.of("success", true, "projectGid", newProjectGid));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('SUPER_USER', 'ADMIN')")
    @PatchMapping("/tasks/{taskGid}")
    public ResponseEntity<?> updateTaskDate(@PathVariable String taskGid, @RequestBody UpdateTaskDateRequest request) {
        try {
            var data = new java.util.HashMap<String, Object>();
            if (request.dueOn() != null) data.put("due_on", request.dueOn());
            if (request.expectedDueDateFieldGid() != null) {
                data.put("custom_fields", Map.of(request.expectedDueDateFieldGid(), request.expectedDueDate()));
            }
            if (request.completed() != null) {
                data.put("completed", request.completed());
            }
            if (data.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "No fields to update"));

            asanaClient.put("/tasks/" + taskGid, data);

            if (request.dueOn() != null) {
                jdbcTemplate.update("UPDATE tasks SET due_on = ? WHERE gid = ?", request.dueOn(), taskGid);
            }
            if (request.expectedDueDateFieldGid() != null) {
                jdbcTemplate.update(
                    "UPDATE task_custom_field_values SET value_text = ? WHERE task_gid = ? AND custom_field_gid = ?",
                    request.expectedDueDate(), taskGid, request.expectedDueDateFieldGid()
                );
            }
            if (request.completed() != null) {
                jdbcTemplate.update("UPDATE tasks SET completed = ? WHERE gid = ?", request.completed(), taskGid);
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update Asana - local DB not changed", "detail", e.getMessage()));
        }
    }
}
