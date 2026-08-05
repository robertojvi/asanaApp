package com.accessparks.asanaapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Read-only for everyone authenticated, including the USER role - reports
// are meant to be broadly viewable, only creation/editing is restricted.
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/project-overview")
    public List<Map<String, Object>> projectOverview() {
        return jdbcTemplate.queryForList("SELECT * FROM v_project_overview ORDER BY total_tasks DESC");
    }

    @GetMapping("/progress-by-corporate")
    public List<Map<String, Object>> progressByCorporate() {
        return jdbcTemplate.queryForList("SELECT * FROM v_progress_by_corporate");
    }

    @GetMapping("/overdue-tasks")
    public List<Map<String, Object>> overdueTasks() {
        return jdbcTemplate.queryForList("SELECT * FROM v_overdue_tasks ORDER BY due_on");
    }
}
