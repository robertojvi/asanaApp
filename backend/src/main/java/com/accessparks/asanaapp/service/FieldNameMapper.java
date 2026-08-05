package com.accessparks.asanaapp.service;

import org.springframework.stereotype.Component;

import java.util.Map;

// Java port of the Node field-map.js - maps raw Asana custom field name
// variants ("Expected Due date", "Expected Date", etc.) to one canonical
// name so reports can group across projects consistently.
@Component
public class FieldNameMapper {

    private static final Map<String, String> ALIASES = Map.ofEntries(
        Map.entry("duration", "duration"),
        Map.entry("lag time", "lag_time"),
        Map.entry("partial status", "partial_status"),
        Map.entry("assign to", "assigned_to"),
        Map.entry("template_task_id", "template_task_id"),
        Map.entry("priority", "priority"),
        Map.entry("task progress", "task_progress"),
        Map.entry("status", "status"),
        Map.entry("expected due date", "expected_due_date"),
        Map.entry("expected date", "expected_due_date"),
        Map.entry("notes", "notes"),
        Map.entry("status notes", "status_notes"),
        Map.entry("index", "sequence_index"),
        Map.entry("corporate", "corporate"),
        Map.entry("units", "units"),
        Map.entry("address", "address"),
        Map.entry("state", "state"),
        Map.entry("city", "city"),
        Map.entry("zip code", "zip_code"),
        Map.entry("mhc", "mhc"),
        Map.entry("pod", "pod"),
        Map.entry("primary contact", "primary_contact"),
        Map.entry("secondary contact", "secondary_contact"),
        Map.entry("mkt profile", "mkt_profile")
        // Extend as needed - unmapped names fall back to a slugified version
        // below, so nothing is lost, just not merged with a canonical group.
    );

    public String canonicalize(String rawName) {
        String key = rawName.trim().toLowerCase().replaceAll("\\s+", " ");
        return ALIASES.getOrDefault(key, slugify(rawName));
    }

    private String slugify(String name) {
        return name.trim().toLowerCase()
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
    }
}
