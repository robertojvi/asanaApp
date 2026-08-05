package com.accessparks.asanaapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// Single-row table holding the current sync schedule, editable at runtime
// from the app instead of requiring a redeploy to change the cron.
@Entity
@Table(name = "sync_config")
@Getter
@Setter
public class SyncConfig {

    @Id
    private Long id = 1L; // fixed single row

    @Column(nullable = false)
    private String cronExpression = "0 0 3 * * *"; // 3am daily

    @Column(nullable = false)
    private boolean enabled = true;

    private String lastRunStatus;
    private String lastRunAt;
}
