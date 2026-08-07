package com.accessparks.asanaapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// A device row nested under a SiteLocation in the "Jira Location/Device List".
// Id is the Jira Assets object id embedded in the device name's link.
@Entity
@Table(name = "site_devices")
@Getter
@Setter
public class SiteDevice {

    @Id
    private Long id;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "device_name")
    private String deviceName;

    private String manufacturer;
    private String model;

    @Column(name = "management_ip")
    private String managementIp;

    @Column(name = "management_mac")
    private String managementMac;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "powered_by")
    private String poweredBy;

    private String height;
    private String azimuth;
    private String tilt;

    @Column(name = "uplink_port")
    private String uplinkPort;

    @Column(name = "connects_to", length = 1000)
    private String connectsTo;

    @Column(length = 1000)
    private String notes;

    private String status;

    @Column(name = "is_e2e")
    private String isE2e;

    @Column(name = "jira_subvenue_key")
    private String jiraSubvenueKey;

    @Column(name = "jira_device_key")
    private String jiraDeviceKey;

    @Column(name = "zabbix_id")
    private String zabbixId;

    @Column(name = "jira_model_key")
    private String jiraModelKey;
}
