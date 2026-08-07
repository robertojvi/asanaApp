package com.accessparks.asanaapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// A row from a site's "Jira Location/Device List" (the Location ID rows,
// backgrounded in MistyRose on the dashboard). Id is the Jira Assets object
// id embedded in the Location's link, used as a stable natural key for upserts.
@Entity
@Table(name = "site_locations")
@Getter
@Setter
public class SiteLocation {

    @Id
    private Long id;

    @Column(name = "subvenue_id")
    private Long subvenueId;

    private String name;
    private String latitude;
    private String longitude;
    private String type;
    private String phase;

    @Column(length = 1000)
    private String notes;
}
