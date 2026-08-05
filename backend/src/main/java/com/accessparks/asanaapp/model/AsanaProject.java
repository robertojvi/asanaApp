package com.accessparks.asanaapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Maps the existing "projects" table (already populated by the Node sync
// scripts / kept in sync going forward by this app's own sync service).
// Read-mostly from the API's perspective - writes happen through the sync
// service, not through arbitrary JPA saves, to avoid drifting from Asana.
@Entity
@Table(name = "projects")
@Getter
@Setter
public class AsanaProject {

    @Id
    @Column(name = "gid")
    private String gid;

    @Column(name = "workspace_gid")
    private String workspaceGid;

    private String name;

    private boolean archived;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
