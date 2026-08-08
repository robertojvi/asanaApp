package com.accessparks.asanaapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// Sticky mapping from a SiteLocation's Jira Assets id to the Asana task gid
// created for it under the site's "Construction Progress" section. Keyed by
// the Jira id (not name) so a later rename of the location in Jira doesn't
// orphan the existing Asana task or cause a duplicate to be created - see
// ConstructionProgressService.
@Entity
@Table(name = "site_location_asana_tasks")
@Getter
@Setter
public class SiteLocationAsanaTask {

    @Id
    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "task_gid", nullable = false)
    private String taskGid;
}
