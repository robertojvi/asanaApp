package com.accessparks.asanaapp.service;

import com.accessparks.asanaapp.model.SyncConfig;
import com.accessparks.asanaapp.repository.SyncConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final TaskScheduler taskScheduler;
    private final SyncConfigRepository syncConfigRepository;
    private final AsanaSyncService asanaSyncService;

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

    /** Runs a sync immediately, outside the schedule (manual trigger from the app). */
    public AsanaSyncService.SyncResult runNow() {
        AsanaSyncService.SyncResult result = asanaSyncService.syncAllTrackedProjects();
        SyncConfig config = getOrCreateConfig();
        config.setLastRunStatus("manual run: " + result.succeeded() + " succeeded, " + result.failed() + " failed");
        config.setLastRunAt(LocalDateTime.now().toString());
        syncConfigRepository.save(config);
        return result;
    }
}
