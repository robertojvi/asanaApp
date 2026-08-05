package com.accessparks.asanaapp.service;

import com.accessparks.asanaapp.model.SyncConfig;
import com.accessparks.asanaapp.repository.SyncConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final TaskScheduler taskScheduler;
    private final SyncConfigRepository syncConfigRepository;
    private final AsanaSyncService asanaSyncService;
    private final SyncProgressTracker progressTracker;

    private ScheduledFuture<?> currentTask;

    @PostConstruct
    public void init() {
        SyncConfig config = getOrCreateConfig();
        if (config.isEnabled()) {
            schedule(config.getCronExpression());
        }
    }

    public SyncConfig getOrCreateConfig() {
        return syncConfigRepository.findById(1L).orElseGet(() -> {
            SyncConfig config = new SyncConfig();
            config.setId(1L);
            return syncConfigRepository.save(config);
        });
    }

    /** Updates the cron expression and reschedules immediately. */
    public SyncConfig updateSchedule(String cronExpression, boolean enabled) {
        SyncConfig config = getOrCreateConfig();
        config.setCronExpression(cronExpression);
        config.setEnabled(enabled);
        syncConfigRepository.save(config);

        if (currentTask != null) {
            currentTask.cancel(false);
            currentTask = null;
        }
        if (enabled) {
            schedule(cronExpression);
        }
        return config;
    }

    private void schedule(String cronExpression) {
        currentTask = taskScheduler.schedule(this::runScheduledSync, new CronTrigger(cronExpression));
    }

    private void runScheduledSync() {
        SyncConfig config = getOrCreateConfig();
        try {
            AsanaSyncService.SyncResult result = asanaSyncService.syncAllTrackedProjects();
            config.setLastRunStatus("succeeded: " + result.succeeded() + " projects, " + result.failed() + " failed");
        } catch (Exception e) {
            config.setLastRunStatus("error: " + e.getMessage());
        }
        config.setLastRunAt(LocalDateTime.now().toString());
        syncConfigRepository.save(config);
    }

    /** Kicks off a sync immediately in the background (manual trigger from the app), so the
     * triggering HTTP request can return right away and the frontend polls SyncProgressTracker
     * for progress. Returns false without starting anything if a sync is already running. */
    public boolean triggerRunNow() {
        if (progressTracker.isRunning()) return false;
        taskScheduler.schedule(() -> {
            SyncConfig config = getOrCreateConfig();
            try {
                AsanaSyncService.SyncResult result = asanaSyncService.syncAllTrackedProjects();
                config.setLastRunStatus("manual run: " + result.succeeded() + " succeeded, " + result.failed() + " failed");
            } catch (Exception e) {
                config.setLastRunStatus("error: " + e.getMessage());
            }
            config.setLastRunAt(LocalDateTime.now().toString());
            syncConfigRepository.save(config);
        }, Instant.now());
        return true;
    }
}
