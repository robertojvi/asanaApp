package com.accessparks.asanaapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "jira_issues")
@Getter
@Setter
public class JiraIssue {

    @Id
    @Column(name = "id")
    private String id;

    // "key" is a reserved word in MySQL, so the column is named issue_key.
    @Column(name = "issue_key", unique = true)
    private String key;

    @Column(name = "project_id")
    private String projectId;

    @Column(length = 1000)
    private String summary;

    @Column(name = "status_name")
    private String statusName;

    @Column(name = "status_category")
    private String statusCategory;

    @Column(name = "issue_type")
    private String issueType;

    private String priority;

    @Column(name = "assignee_name")
    private String assigneeName;

    @Column(name = "reporter_name")
    private String reporterName;

    @Column(name = "due_date")
    private LocalDate dueDate;

    private LocalDateTime created;
    private LocalDateTime updated;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
}
