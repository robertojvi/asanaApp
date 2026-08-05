package com.accessparks.asanaapp.controller;

import com.accessparks.asanaapp.model.SyncConfig;
import com.accessparks.asanaapp.service.AsanaSyncService;
import com.accessparks.asanaapp.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_USER', 'ADMIN')")
public class SyncController {

    private final SchedulerService schedulerService;

    @GetMapping("/config")
    public SyncConfig getConfig() {
        return schedulerService.getOrCreateConfig();
    }

    public record UpdateScheduleRequest(String cronExpression, boolean enabled) {}

    @PutMapping("/config")
    public SyncConfig updateConfig(@RequestBody UpdateScheduleRequest request) {
        return schedulerService.updateSchedule(request.cronExpression(), request.enabled());
    }

    @PostMapping("/run-now")
    public AsanaSyncService.SyncResult runNow() {
        return schedulerService.runNow();
    }
}
