package com.accessparks.asanaapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Getter
@Setter
public class AsanaTask {

    @Id
    @Column(name = "gid")
    private String gid;

    @Column(name = "project_gid")
    private String projectGid;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private boolean completed;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "assignee_gid")
    private String assigneeGid;

    @Column(name = "due_on")
    private LocalDate dueOn;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
