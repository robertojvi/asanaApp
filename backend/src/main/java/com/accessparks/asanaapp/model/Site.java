package com.accessparks.asanaapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Mirrors a "subvenue" from the internal site inventory dashboard
// (http://192.168.102.32/views/sitelist.php). Well-known fields are promoted
// to columns for querying; anything else captured from the page is kept in
// rawFieldsJson so a scrape never silently loses data.
//
// Nearly every column is TEXT rather than the default VARCHAR(255): with ~40
// text columns, VARCHAR's fixed-length row accounting blew past InnoDB's
// 65535-byte row-size limit and silently failed to create the table. TEXT
// columns are stored off-page and only count a small pointer toward that
// limit, and none of these fields need indexing/filtering (subvenueName is
// only ever sorted, which TEXT still supports fine at this row count).
@Entity
@Table(name = "sites")
@Getter
@Setter
public class Site {

    @Id
    @Column(name = "subvenue_id")
    private Long subvenueId;

    @Column(name = "venue_key", columnDefinition = "TEXT")
    private String venueKey;

    @Column(name = "subvenue_key", columnDefinition = "TEXT")
    private String subvenueKey;

    // Asana project gid, scraped from the "Proj" column's app.asana.com/0/{gid}
    // link on sitelist.php. Not every site has one (e.g. decommissioned/spare
    // sites usually don't) - nullable by design.
    @Column(name = "asana_project_gid")
    private String asanaProjectGid;

    @Column(name = "venue_type", columnDefinition = "TEXT")
    private String venueType;

    @Column(name = "venue_name", columnDefinition = "TEXT")
    private String venueName;

    @Column(name = "subvenue_name", columnDefinition = "TEXT")
    private String subvenueName;

    @Column(columnDefinition = "TEXT")
    private String abbr;

    @Column(columnDefinition = "TEXT")
    private String logo;

    @Column(columnDefinition = "TEXT")
    private String region;

    @Column(name = "time_zone", columnDefinition = "TEXT")
    private String timeZone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String website;

    @Column(name = "front_desk_phone", columnDefinition = "TEXT")
    private String frontDeskPhone;

    @Column(name = "operating_season", columnDefinition = "TEXT")
    private String operatingSeason;

    @Column(name = "office_hours", columnDefinition = "TEXT")
    private String officeHours;

    @Column(name = "lot_count", columnDefinition = "TEXT")
    private String lotCount;

    @Column(columnDefinition = "TEXT")
    private String owner;

    @Column(columnDefinition = "TEXT")
    private String managers;

    @Column(name = "other_staff", columnDefinition = "TEXT")
    private String otherStaff;

    @Column(name = "billing_contact", columnDefinition = "TEXT")
    private String billingContact;

    @Column(name = "dispatch_contact", columnDefinition = "TEXT")
    private String dispatchContact;

    @Column(name = "after_hours_contact", columnDefinition = "TEXT")
    private String afterHoursContact;

    @Column(name = "power_checks_contact", columnDefinition = "TEXT")
    private String powerChecksContact;

    @Column(name = "internal_it", columnDefinition = "TEXT")
    private String internalIt;

    @Column(name = "isp", columnDefinition = "TEXT")
    private String isp;

    @Column(name = "electric_utility", columnDefinition = "TEXT")
    private String electricUtility;

    @Column(name = "smart_hands", columnDefinition = "TEXT")
    private String smartHands;

    @Column(name = "contractors", columnDefinition = "TEXT")
    private String contractors;

    @Column(name = "sales_engineer", columnDefinition = "TEXT")
    private String salesEngineer;

    @Column(name = "account_manager", columnDefinition = "TEXT")
    private String accountManager;

    @Column(name = "field_ops_manager", columnDefinition = "TEXT")
    private String fieldOpsManager;

    @Column(name = "wifi_engineer", columnDefinition = "TEXT")
    private String wifiEngineer;

    @Column(name = "backhaul_engineer", columnDefinition = "TEXT")
    private String backhaulEngineer;

    @Column(name = "build_engineer", columnDefinition = "TEXT")
    private String buildEngineer;

    @Column(name = "test_engineer", columnDefinition = "TEXT")
    private String testEngineer;

    @Column(name = "support_engineer", columnDefinition = "TEXT")
    private String supportEngineer;

    @Column(name = "launch_status", columnDefinition = "TEXT")
    private String launchStatus;

    @Column(name = "billing_model", columnDefinition = "TEXT")
    private String billingModel;

    @Column(name = "walled_garden_mode", columnDefinition = "TEXT")
    private String walledGardenMode;

    @Column(name = "slas_offered", columnDefinition = "TEXT")
    private String slasOffered;

    @Column(name = "ipv6_enabled", columnDefinition = "TEXT")
    private String ipv6Enabled;

    @Column(name = "ipv6_prefix", columnDefinition = "TEXT")
    private String ipv6Prefix;

    @Column(columnDefinition = "TEXT")
    private String ssids;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "backhaul_tech_stacks", columnDefinition = "TEXT")
    private String backhaulTechStacks;

    @Column(columnDefinition = "TEXT")
    private String pricing;

    // Full label->value map captured from siteinfo.php, as JSON, so nothing
    // parsed off the page is ever lost even if a field above misparses.
    @Column(name = "raw_fields_json", columnDefinition = "MEDIUMTEXT")
    private String rawFieldsJson;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
}
