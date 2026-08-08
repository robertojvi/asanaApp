package com.accessparks.asanaapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// Sticky mapping from a SiteDevice's Jira Assets id to the Asana subtask gid
// created for it. Keyed by the Jira id (not name) so a later rename of the
// device in Jira doesn't orphan the existing Asana subtask or cause a
// duplicate to be created - see ConstructionProgressService.
@Entity
@Table(name = "site_device_asana_tasks")
@Getter
@Setter
public class SiteDeviceAsanaTask {

    @Id
    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "subtask_gid", nullable = false)
    private String subtaskGid;
}
