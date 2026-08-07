package com.accessparks.asanaapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// New tables owned entirely by this app (no legacy Node sync tool involved,
// unlike the Asana tables) - Hibernate manages the schema via ddl-auto: update.
@Entity
@Table(name = "jira_projects")
@Getter
@Setter
public class JiraProject {

    @Id
    @Column(name = "id")
    private String id;

    // "key" is a reserved word in MySQL, so the column is named project_key.
    @Column(name = "project_key", unique = true)
    private String key;

    private String name;

    @Column(name = "project_type_key")
    private String projectTypeKey;

    @Column(name = "lead_name")
    private String leadName;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
}
