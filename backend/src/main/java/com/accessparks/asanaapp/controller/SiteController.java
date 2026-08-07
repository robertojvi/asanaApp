package com.accessparks.asanaapp.controller;

import com.accessparks.asanaapp.model.Site;
import com.accessparks.asanaapp.model.SiteDevice;
import com.accessparks.asanaapp.model.SiteLocation;
import com.accessparks.asanaapp.repository.SiteDeviceRepository;
import com.accessparks.asanaapp.repository.SiteLocationRepository;
import com.accessparks.asanaapp.repository.SiteRepository;
import com.accessparks.asanaapp.service.SiteInventorySyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteRepository siteRepository;
    private final SiteLocationRepository locationRepository;
    private final SiteDeviceRepository deviceRepository;
    private final SiteInventorySyncService siteInventorySyncService;

    @GetMapping
    public List<Site> listSites() {
        return siteRepository.findAll(Sort.by("subvenueName"));
    }

    @GetMapping("/{subvenueId}/locations")
    public List<SiteLocation> listLocations(@PathVariable Long subvenueId) {
        return locationRepository.findBySubvenueIdInOrderByNameAsc(List.of(subvenueId));
    }

    @GetMapping("/devices")
    public List<SiteDevice> listDevices(@RequestParam("location_ids") String locationIdsCsv) {
        List<Long> locationIds = List.of(locationIdsCsv.split(",")).stream().map(Long::valueOf).toList();
        if (locationIds.isEmpty()) return List.of();
        return deviceRepository.findByLocationIdInOrderByDeviceNameAsc(locationIds);
    }

    @PreAuthorize("hasAnyRole('SUPER_USER', 'ADMIN')")
    @PostMapping("/sync")
    public ResponseEntity<?> syncNow() {
        try {
            SiteInventorySyncService.SyncResult result = siteInventorySyncService.syncAll();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "sitesSynced", result.sitesSynced(),
                "locationsSynced", result.locationsSynced(),
                "devicesSynced", result.devicesSynced(),
                "errors", result.errors()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
