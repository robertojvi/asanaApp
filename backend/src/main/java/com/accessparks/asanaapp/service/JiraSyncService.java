package com.accessparks.asanaapp.service;

import com.accessparks.asanaapp.model.JiraIssue;
import com.accessparks.asanaapp.model.JiraProject;
import com.accessparks.asanaapp.repository.JiraIssueRepository;
import com.accessparks.asanaapp.repository.JiraProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JiraSyncService {

    private final JiraClient jiraClient;
    private final JiraProjectRepository projectRepository;
    private final JiraIssueRepository issueRepository;

    // Request-scoped (open-in-view) persistence context never gets cleared on its
    // own, so saving thousands of issues in one request makes every subsequent
    // save() dirty-check an ever-growing first-level cache (O(n^2)). Periodic
    // clear() below keeps that cache bounded.
    @PersistenceContext
    private EntityManager entityManager;

    private static final int FLUSH_EVERY = 200;

    private static final String ISSUE_FIELDS = String.join(",",
        "summary", "status", "issuetype", "priority", "assignee", "reporter", "duedate", "created", "updated"
    );

    // Jira Cloud timestamps look like "2026-08-01T10:15:30.000+0000".
    private static final DateTimeFormatter JIRA_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    /** Pulls every project and all of their issues from Jira into MySQL. */
    public SyncResult syncAll() {
        int projectsSynced = 0;
        int issuesSynced = 0;
        List<String> errors = new ArrayList<>();

        for (JsonNode projectNode : getAllProjectPages()) {
            String projectId = projectNode.get("id").asText();
            try {
                upsertProject(projectNode);
                projectsSynced++;
                for (JsonNode issueNode : getAllIssuePages(projectNode.get("key").asText())) {
                    upsertIssue(issueNode, projectId);
                    issuesSynced++;
                    if (issuesSynced % FLUSH_EVERY == 0) {
                        entityManager.clear();
                    }
                }
            } catch (Exception e) {
                errors.add(projectNode.get("key").asText() + ": " + e.getMessage());
            }
        }
        return new SyncResult(projectsSynced, issuesSynced, errors);
    }

    private List<JsonNode> getAllProjectPages() {
        List<JsonNode> all = new ArrayList<>();
        int startAt = 0;
        while (true) {
            JsonNode page = jiraClient.get("/project/search", Map.of(
                "startAt", String.valueOf(startAt), "maxResults", "50"
            ));
            page.get("values").forEach(all::add);
            if (page.get("isLast").asBoolean()) break;
            startAt += page.get("maxResults").asInt();
        }
        return all;
    }

    private List<JsonNode> getAllIssuePages(String projectKey) {
        List<JsonNode> all = new ArrayList<>();
        // Atlassian retired GET /search (returns 410) in favor of /search/jql,
        // which pages via an opaque nextPageToken cursor instead of startAt/total.
        String nextPageToken = null;
        while (true) {
            Map<String, String> params = new HashMap<>();
            // Project keys can collide with JQL reserved words (e.g. "AS"), so
            // the key must always be quoted.
            params.put("jql", "project = \"" + projectKey + "\" ORDER BY key");
            params.put("fields", ISSUE_FIELDS);
            params.put("maxResults", "100");
            if (nextPageToken != null) params.put("nextPageToken", nextPageToken);

            JsonNode page = jiraClient.get("/search/jql", params);
            page.get("issues").forEach(all::add);

            JsonNode tokenNode = page.get("nextPageToken");
            nextPageToken = (tokenNode != null && !tokenNode.isNull()) ? tokenNode.asText() : null;
            boolean isLast = !page.has("isLast") || page.get("isLast").asBoolean();
            if (isLast || nextPageToken == null) break;
        }
        return all;
    }

    private void upsertProject(JsonNode p) {
        JiraProject project = projectRepository.findById(p.get("id").asText()).orElseGet(JiraProject::new);
        project.setId(p.get("id").asText());
        project.setKey(p.get("key").asText());
        project.setName(p.get("name").asText());
        project.setProjectTypeKey(textOrNull(p, "projectTypeKey"));

        JsonNode lead = p.get("lead");
        project.setLeadName(lead != null && !lead.isNull() ? textOrNull(lead, "displayName") : null);

        JsonNode avatarUrls = p.get("avatarUrls");
        project.setAvatarUrl(avatarUrls != null && !avatarUrls.isNull() ? textOrNull(avatarUrls, "48x48") : null);

        project.setLastSyncedAt(LocalDateTime.now());
        projectRepository.save(project);
    }

    private void upsertIssue(JsonNode i, String projectId) {
        JsonNode fields = i.get("fields");

        JiraIssue issue = issueRepository.findById(i.get("id").asText()).orElseGet(JiraIssue::new);
        issue.setId(i.get("id").asText());
        issue.setKey(i.get("key").asText());
        issue.setProjectId(projectId);
        issue.setSummary(textOrNull(fields, "summary"));

        JsonNode status = fields.get("status");
        issue.setStatusName(status != null && !status.isNull() ? textOrNull(status, "name") : null);
        JsonNode statusCategory = status != null ? status.get("statusCategory") : null;
        issue.setStatusCategory(statusCategory != null && !statusCategory.isNull() ? textOrNull(statusCategory, "name") : null);

        JsonNode issueType = fields.get("issuetype");
        issue.setIssueType(issueType != null && !issueType.isNull() ? textOrNull(issueType, "name") : null);

        JsonNode priority = fields.get("priority");
        issue.setPriority(priority != null && !priority.isNull() ? textOrNull(priority, "name") : null);

        JsonNode assignee = fields.get("assignee");
        issue.setAssigneeName(assignee != null && !assignee.isNull() ? textOrNull(assignee, "displayName") : null);

        JsonNode reporter = fields.get("reporter");
        issue.setReporterName(reporter != null && !reporter.isNull() ? textOrNull(reporter, "displayName") : null);

        String dueDate = textOrNull(fields, "duedate");
        issue.setDueDate(dueDate != null ? LocalDate.parse(dueDate) : null);

        issue.setCreated(toDateTime(textOrNull(fields, "created")));
        issue.setUpdated(toDateTime(textOrNull(fields, "updated")));
        issue.setLastSyncedAt(LocalDateTime.now());

        issueRepository.save(issue);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private LocalDateTime toDateTime(String iso) {
        if (iso == null) return null;
        return OffsetDateTime.parse(iso, JIRA_DATETIME).toLocalDateTime();
    }

    public record SyncResult(int projectsSynced, int issuesSynced, List<String> errors) {}
}
