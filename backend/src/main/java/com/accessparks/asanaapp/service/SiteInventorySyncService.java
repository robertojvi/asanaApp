package com.accessparks.asanaapp.service;

import com.accessparks.asanaapp.model.Site;
import com.accessparks.asanaapp.model.SiteDevice;
import com.accessparks.asanaapp.model.SiteLocation;
import com.accessparks.asanaapp.repository.SiteDeviceRepository;
import com.accessparks.asanaapp.repository.SiteLocationRepository;
import com.accessparks.asanaapp.repository.SiteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SiteInventorySyncService {

    private final SiteInventoryClient client;
    private final SiteRepository siteRepository;
    private final SiteLocationRepository locationRepository;
    private final SiteDeviceRepository deviceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Same lesson as the Jira sync: with open-in-view on, a single request's
    // persistence context never clears itself, so saving thousands of
    // locations/devices in one request makes every later save() dirty-check an
    // ever-growing first-level cache. Clear it periodically.
    @PersistenceContext
    private EntityManager entityManager;

    private static final int FLUSH_EVERY = 200;

    private static final Pattern SUBVENUE_ID_PATTERN = Pattern.compile("subvenue_id=(\\d+)");
    private static final Pattern OBJECT_ID_PATTERN = Pattern.compile("/assets/object/(\\d+)");
    private static final Pattern VENUE_KEY_PATTERN = Pattern.compile("Venue:(IHS-\\d+)");
    private static final Pattern SUBVENUE_KEY_PATTERN = Pattern.compile("Subvenue:(IHS-\\d+)");
    private static final Pattern ASANA_PROJECT_PATTERN = Pattern.compile("app\\.asana\\.com/0/(\\d+)");

    public SyncResult syncAll() {
        List<SiteRow> siteRows = parseSiteList(client.fetchSiteList());

        int sitesSynced = 0, locationsSynced = 0, devicesSynced = 0;
        List<String> errors = new ArrayList<>();

        for (SiteRow row : siteRows) {
            try {
                Map<String, String> fields = parseSiteInfoFields(client.fetchSiteInfo(row.id(), row.name()));
                upsertSite(row.id(), row.name(), row.asanaProjectGid(), fields);
                sitesSynced++;

                List<ParsedLocation> locations = parseLocationsAndDevices(client.fetchSubvenueDetail(row.id(), row.name()));
                for (ParsedLocation loc : locations) {
                    if (loc.id() == null) continue;
                    upsertLocation(row.id(), loc);
                    locationsSynced++;

                    for (ParsedDevice dev : loc.devices()) {
                        if (dev.id() == null) continue;
                        upsertDevice(loc.id(), dev);
                        devicesSynced++;
                        if (devicesSynced % FLUSH_EVERY == 0) {
                            entityManager.clear();
                        }
                    }
                }
            } catch (Exception e) {
                errors.add(row.id() + " (" + row.name() + "): " + e.getMessage());
            }
        }
        return new SyncResult(sitesSynced, locationsSynced, devicesSynced, errors);
    }

    // ---- sitelist.php ----

    private List<SiteRow> parseSiteList(Document doc) {
        List<SiteRow> rows = new ArrayList<>();
        Elements trs = doc.select("table#myTable tbody tr");
        for (Element tr : trs) {
            Elements cells = tr.select("> td");
            if (cells.size() < 3) continue;

            String rowHtml = tr.outerHtml();
            Matcher m = SUBVENUE_ID_PATTERN.matcher(rowHtml);
            if (!m.find()) continue;
            Long id = Long.valueOf(m.group(1));
            String name = cells.get(2).text().trim();

            Matcher am = ASANA_PROJECT_PATTERN.matcher(rowHtml);
            String asanaProjectGid = am.find() ? am.group(1) : null;

            rows.add(new SiteRow(id, name, asanaProjectGid));
        }
        // dedupe by id (each row links the same subvenue_id from several columns,
        // but there's exactly one <tr> per site so this is just a safety net)
        Map<Long, SiteRow> byId = new LinkedHashMap<>();
        for (SiteRow r : rows) byId.putIfAbsent(r.id(), r);
        return new ArrayList<>(byId.values());
    }

    // ---- siteinfo.php ----

    private Map<String, String> parseSiteInfoFields(Document doc) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (Element row : doc.select("tr")) {
            Elements cells = row.select("> td");
            if (cells.size() < 2) continue;
            String label = cells.get(0).text().trim();
            if (!label.endsWith(":") || label.length() < 2) continue;
            label = label.substring(0, label.length() - 1).trim();
            String value = cells.get(1).text().trim();
            fields.putIfAbsent(label, value);
        }

        String pageText = doc.text();
        Matcher vm = VENUE_KEY_PATTERN.matcher(pageText);
        if (vm.find()) fields.putIfAbsent("_venueKey", vm.group(1));
        Matcher sm = SUBVENUE_KEY_PATTERN.matcher(pageText);
        if (sm.find()) fields.putIfAbsent("_subvenueKey", sm.group(1));

        return fields;
    }

    private void upsertSite(Long subvenueId, String fallbackName, String asanaProjectGid, Map<String, String> f) {
        Site site = siteRepository.findById(subvenueId).orElseGet(Site::new);
        site.setSubvenueId(subvenueId);
        site.setAsanaProjectGid(asanaProjectGid);
        site.setVenueKey(f.get("_venueKey"));
        site.setSubvenueKey(f.get("_subvenueKey"));
        site.setVenueType(f.get("Venue_type"));
        site.setVenueName(f.getOrDefault("Venue", ""));
        site.setSubvenueName(blankToFallback(f.get("Subvenue"), fallbackName));
        site.setAbbr(f.get("Abbr"));
        site.setLogo(f.get("Logo"));
        site.setRegion(f.get("Region"));
        site.setTimeZone(f.get("Time Zone"));
        site.setAddress(f.get("Address"));
        site.setWebsite(f.get("Website"));
        site.setFrontDeskPhone(f.get("Front Desk Phone"));
        site.setOperatingSeason(f.get("Operating Season"));
        site.setOfficeHours(f.get("Office Hours"));
        site.setLotCount(f.get("Lot_Count"));
        site.setOwner(f.get("Owner"));
        site.setManagers(f.get("Managers"));
        site.setOtherStaff(f.get("Other Staff"));
        site.setBillingContact(f.get("Billing Contact"));
        site.setDispatchContact(f.get("Dispatch Maintenance Outage Notifications"));
        site.setAfterHoursContact(f.get("After Hours Contact"));
        site.setPowerChecksContact(f.get("Power Checks"));
        site.setInternalIt(f.get("Internal IT"));
        site.setIsp(f.get("Internet Service Provider"));
        site.setElectricUtility(f.get("Electric Utility"));
        site.setSmartHands(f.get("Smart Hands"));
        site.setContractors(truncate(f.get("Contractors"), 2000));
        site.setSalesEngineer(f.get("Sales Engineer"));
        site.setAccountManager(f.get("Account Manager"));
        site.setFieldOpsManager(f.get("Field Operations Manager"));
        site.setWifiEngineer(f.get("Wifi Engineer"));
        site.setBackhaulEngineer(f.get("Backhaul Engineer"));
        site.setBuildEngineer(f.get("Build Engineer"));
        site.setTestEngineer(f.get("Test Engineer"));
        site.setSupportEngineer(f.get("Support Engineer"));
        site.setLaunchStatus(f.get("Launch_Status"));
        site.setBillingModel(f.get("Billing_Model"));
        site.setWalledGardenMode(f.get("Walled_Garden_Mode"));
        site.setSlasOffered(f.get("SLAs Offered"));
        site.setIpv6Enabled(f.get("IPv6 enabled"));
        site.setIpv6Prefix(f.get("IPv6_prefix"));
        site.setSsids(f.get("SSIDs"));
        site.setNotes(truncate(f.get("Notes"), 4000));
        site.setBackhaulTechStacks(f.get("Backhaul Technology Stacks"));
        site.setPricing(truncate(f.get("Pricing"), 1000));
        site.setRawFieldsJson(toJson(f));
        site.setLastSyncedAt(LocalDateTime.now());
        siteRepository.save(site);
    }

    // ---- subvenue.php: Jira Location/Device List ----

    private List<ParsedLocation> parseLocationsAndDevices(Document doc) {
        Element locationIdCell = null;
        for (Element cell : doc.select("th, td")) {
            if (cell.text().trim().equalsIgnoreCase("Location ID")) {
                locationIdCell = cell;
                break;
            }
        }
        if (locationIdCell == null) return List.of();

        Element table = locationIdCell.closest("table");
        if (table == null) return List.of();

        List<ParsedLocation> locations = new ArrayList<>();
        List<ParsedDevice> currentDevices = null;
        Long currentLocationId = null;
        String currentName = null, currentLat = null, currentLon = null, currentType = null, currentPhase = null, currentNotes = null;
        boolean inDeviceRows = false;

        for (Element row : table.select("tr")) {
            Elements cells = row.select("td, th");
            if (cells.isEmpty()) continue;
            String rowText = row.text();

            if (rowText.contains("Location ID")) { inDeviceRows = false; continue; }
            if (rowText.contains("device_name")) { inDeviceRows = true; continue; }

            String firstCellStyle = cells.first().attr("style");
            if (firstCellStyle.contains("MistyRose")) {
                // flush previous location before starting a new one
                if (currentLocationId != null) {
                    locations.add(new ParsedLocation(currentLocationId, currentName, currentLat, currentLon, currentType, currentPhase, currentNotes, currentDevices));
                }
                inDeviceRows = false;
                currentDevices = new ArrayList<>();

                Element idCell = cells.size() > 1 ? cells.get(1) : null;
                Element link = idCell != null ? idCell.selectFirst("a") : null;
                currentLocationId = extractObjectId(link);
                currentName = idCell != null ? idCell.text().trim() : null;
                currentLat = cellText(cells, 2);
                currentLon = cellText(cells, 3);
                currentType = cellText(cells, 4);
                currentPhase = cellText(cells, 5);
                currentNotes = cellText(cells, 6);
                continue;
            }

            if (inDeviceRows && currentDevices != null && cells.size() >= 13) {
                Element statusImg = row.selectFirst("img");
                String status = statusImg != null ? statusImg.attr("data-status") : null;
                String isE2e = statusImg != null ? statusImg.attr("data-is-e2e") : null;
                String jiraSubvenueKey = statusImg != null ? statusImg.attr("data-jira-subvenue-key") : null;
                String jiraDeviceKey = statusImg != null ? statusImg.attr("data-jira-device-key") : null;
                String zabbixId = statusImg != null ? statusImg.attr("data-zabbix-id") : null;
                String jiraModelKey = statusImg != null ? statusImg.attr("data-jira-model-key") : null;

                Element devLink = cells.get(1).selectFirst("a");
                Long deviceId = extractObjectId(devLink);
                String deviceName = cells.get(1).text().trim();

                currentDevices.add(new ParsedDevice(
                    deviceId, deviceName,
                    cellText(cells, 2), cellText(cells, 3), cellText(cells, 4), cellText(cells, 5),
                    cellText(cells, 6), cellText(cells, 7), cellText(cells, 8), cellText(cells, 9),
                    cellText(cells, 10), cellText(cells, 11),
                    cells.size() > 13 ? cellText(cells, 13) : null,
                    status, isE2e, jiraSubvenueKey, jiraDeviceKey, zabbixId, jiraModelKey,
                    cellText(cells, 12)
                ));
            }
        }
        if (currentLocationId != null) {
            locations.add(new ParsedLocation(currentLocationId, currentName, currentLat, currentLon, currentType, currentPhase, currentNotes, currentDevices));
        }
        return locations;
    }

    private void upsertLocation(Long subvenueId, ParsedLocation loc) {
        SiteLocation entity = locationRepository.findById(loc.id()).orElseGet(SiteLocation::new);
        entity.setId(loc.id());
        entity.setSubvenueId(subvenueId);
        entity.setName(loc.name());
        entity.setLatitude(loc.latitude());
        entity.setLongitude(loc.longitude());
        entity.setType(loc.type());
        entity.setPhase(loc.phase());
        entity.setNotes(truncate(loc.notes(), 1000));
        locationRepository.save(entity);
    }

    private void upsertDevice(Long locationId, ParsedDevice dev) {
        SiteDevice entity = deviceRepository.findById(dev.id()).orElseGet(SiteDevice::new);
        entity.setId(dev.id());
        entity.setLocationId(locationId);
        entity.setDeviceName(dev.deviceName());
        entity.setManufacturer(dev.manufacturer());
        entity.setModel(dev.model());
        entity.setManagementIp(dev.managementIp());
        entity.setManagementMac(dev.managementMac());
        entity.setSerialNumber(dev.serialNumber());
        entity.setPoweredBy(dev.poweredBy());
        entity.setHeight(dev.height());
        entity.setAzimuth(dev.azimuth());
        entity.setTilt(dev.tilt());
        entity.setUplinkPort(dev.uplinkPort());
        entity.setConnectsTo(truncate(dev.connectsTo(), 1000));
        entity.setNotes(truncate(dev.notes(), 1000));
        entity.setStatus(dev.status());
        entity.setIsE2e(dev.isE2e());
        entity.setJiraSubvenueKey(dev.jiraSubvenueKey());
        entity.setJiraDeviceKey(dev.jiraDeviceKey());
        entity.setZabbixId(dev.zabbixId());
        entity.setJiraModelKey(dev.jiraModelKey());
        deviceRepository.save(entity);
    }

    // ---- helpers ----

    private Long extractObjectId(Element link) {
        if (link == null) return null;
        Matcher m = OBJECT_ID_PATTERN.matcher(link.attr("href"));
        return m.find() ? Long.valueOf(m.group(1)) : null;
    }

    private String cellText(Elements cells, int index) {
        if (index >= cells.size()) return null;
        String t = cells.get(index).text().trim();
        return t.isEmpty() ? null : t;
    }

    private String blankToFallback(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private String toJson(Map<String, String> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            return null;
        }
    }

    private record SiteRow(Long id, String name, String asanaProjectGid) {}

    private record ParsedLocation(Long id, String name, String latitude, String longitude, String type,
                                   String phase, String notes, List<ParsedDevice> devices) {}

    private record ParsedDevice(Long id, String deviceName, String manufacturer, String model, String managementIp,
                                 String managementMac, String serialNumber, String poweredBy, String height,
                                 String azimuth, String tilt, String uplinkPort, String notes, String status,
                                 String isE2e, String jiraSubvenueKey, String jiraDeviceKey, String zabbixId,
                                 String jiraModelKey, String connectsTo) {}

    public record SyncResult(int sitesSynced, int locationsSynced, int devicesSynced, List<String> errors) {}
}
