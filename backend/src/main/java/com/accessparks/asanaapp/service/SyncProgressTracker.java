package com.accessparks.asanaapp.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Shared in-memory progress state for the currently running sync, if any, so the
 * frontend can poll GET /api/sync/progress and render a progress bar. */
@Component
public class SyncProgressTracker {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger total = new AtomicInteger(0);
    private final AtomicInteger completed = new AtomicInteger(0);
    private final AtomicInteger failed = new AtomicInteger(0);

    /** Atomically claims the "running" state. Returns false if a sync is already in progress. */
    public boolean tryStart(int totalProjects) {
        if (!running.compareAndSet(false, true)) return false;
        total.set(totalProjects);
        completed.set(0);
        failed.set(0);
        return true;
    }

    public void recordResult(boolean success) {
        completed.incrementAndGet();
        if (!success) failed.incrementAndGet();
    }

    public void finish() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public record Snapshot(boolean running, int total, int completed, int failed) {}

    public Snapshot snapshot() {
        return new Snapshot(running.get(), total.get(), completed.get(), failed.get());
    }
}
