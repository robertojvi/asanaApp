package com.accessparks.asanaapp.controller;

import com.accessparks.asanaapp.model.JiraIssue;
import com.accessparks.asanaapp.model.JiraProject;
import com.accessparks.asanaapp.repository.JiraIssueRepository;
import com.accessparks.asanaapp.repository.JiraProjectRepository;
import com.accessparks.asanaapp.service.JiraSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jira")
@RequiredArgsConstructor
public class JiraController {

    private final JiraProjectRepository projectRepository;
    private final JiraIssueRepository issueRepository;
    private final JiraSyncService jiraSyncService;

    @GetMapping("/projects")
    public List<JiraProject> listProjects() {
        return projectRepository.findAll(Sort.by("name"));
    }

    @GetMapping("/issues")
    public List<JiraIssue> listIssues(@RequestParam("project_ids") String projectIdsCsv) {
        List<String> projectIds = List.of(projectIdsCsv.split(","));
        if (projectIds.isEmpty()) return List.of();
        return issueRepository.findByProjectIdInOrderByKeyAsc(projectIds);
    }

    @PreAuthorize("hasAnyRole('SUPER_USER', 'ADMIN')")
    @PostMapping("/sync")
    public ResponseEntity<?> syncNow() {
        try {
            JiraSyncService.SyncResult result = jiraSyncService.syncAll();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "projectsSynced", result.projectsSynced(),
                "issuesSynced", result.issuesSynced(),
                "errors", result.errors()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
