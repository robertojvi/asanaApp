package com.accessparks.asanaapp.controller;

import com.accessparks.asanaapp.model.SyncConfig;
import com.accessparks.asanaapp.service.SchedulerService;
import com.accessparks.asanaapp.service.SyncProgressTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_USER', 'ADMIN')")
public class SyncController {

    private final SchedulerService schedulerService;
    private final SyncProgressTracker progressTracker;

    @GetMapping("/config")
    public SyncConfig getConfig() {
        return schedulerService.getOrCreateConfig();
    }

    public record UpdateScheduleRequest(String cronExpression, boolean enabled) {}

    @PutMapping("/config")
    public SyncConfig updateConfig(@RequestBody UpdateScheduleRequest request) {
        return schedulerService.updateSchedule(request.cronExpression(), request.enabled());
    }

    /** Kicks off a sync in the background and returns immediately; poll GET /progress for status. */
    @PostMapping("/run-now")
    public ResponseEntity<?> runNow() {
        if (!schedulerService.triggerRunNow()) {
            return ResponseEntity.status(409).body(Map.of("error", "A sync is already running"));
        }
        return ResponseEntity.accepted().body(Map.of("message", "Sync started"));
    }

    @GetMapping("/progress")
    public SyncProgressTracker.Snapshot progress() {
        return progressTracker.snapshot();
    }
}
