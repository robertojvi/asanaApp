package com.accessparks.asanaapp.service;

import com.accessparks.asanaapp.model.Site;
import com.accessparks.asanaapp.model.SiteDevice;
import com.accessparks.asanaapp.model.SiteDeviceAsanaTask;
import com.accessparks.asanaapp.model.SiteLocation;
import com.accessparks.asanaapp.model.SiteLocationAsanaTask;
import com.accessparks.asanaapp.repository.SiteDeviceAsanaTaskRepository;
import com.accessparks.asanaapp.repository.SiteDeviceRepository;
import com.accessparks.asanaapp.repository.SiteLocationAsanaTaskRepository;
import com.accessparks.asanaapp.repository.SiteLocationRepository;
import com.accessparks.asanaapp.repository.SiteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Mirrors a site's "Jira Location/Device List" into a "Construction Progress"
// section of its linked Asana project: one task per Location (e.g. "Cabin 1",
// "Pole 10") with one subtask per device, completed iff the device's status is
// "In Service". A parent task is marked complete once every one of its
// subtasks is complete, and reopened if any device regresses out of service.
//
// Re-running this (e.g. after a Jira sync changes device statuses) updates
// tasks/subtasks in place rather than duplicating them. Matching is done by
// the Jira Assets id, persisted in SiteLocationAsanaTask/SiteDeviceAsanaTask,
// not by name - a location or device can be renamed in Jira after its Asana
// task/subtask was created without orphaning it. Tasks/subtasks created by an
// older version of this feature (before the id mapping existed) are picked up
// once by matching on their current name, and a mapping row is backfilled.
@Service
@RequiredArgsConstructor
public class ConstructionProgressService {

    private static final String SECTION_NAME = "Construction Progress";
    private static final String IN_SERVICE_STATUS = "In Service";

    private final AsanaClient asanaClient;
    private final SiteRepository siteRepository;
    private final SiteLocationRepository locationRepository;
    private final SiteDeviceRepository deviceRepository;
    private final SiteLocationAsanaTaskRepository locationAsanaTaskRepository;
    private final SiteDeviceAsanaTaskRepository deviceAsanaTaskRepository;
    private final AsanaSyncService asanaSyncService;

    public Result generate(Long subvenueId) {
        Site site = siteRepository.findById(subvenueId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown site: " + subvenueId));
        String projectGid = site.getAsanaProjectGid();
        if (projectGid == null || projectGid.isBlank()) {
            throw new IllegalStateException("This site has no linked Asana project");
        }

        List<SiteLocation> locations = locationRepository.findBySubvenueIdInOrderByNameAsc(List.of(subvenueId));
        List<Long> locationIds = locations.stream().map(SiteLocation::getId).toList();

        Map<Long, List<SiteDevice>> devicesByLocation = new HashMap<>();
        List<SiteDevice> allDevices = locationIds.isEmpty() ? List.of() : deviceRepository.findByLocationIdInOrderByDeviceNameAsc(locationIds);
        for (SiteDevice device : allDevices) {
            devicesByLocation.computeIfAbsent(device.getLocationId(), k -> new ArrayList<>()).add(device);
        }

        Map<Long, String> taskGidByLocationId = new HashMap<>();
        for (SiteLocationAsanaTask mapping : locationAsanaTaskRepository.findAllById(locationIds)) {
            taskGidByLocationId.put(mapping.getLocationId(), mapping.getTaskGid());
        }
        List<Long> deviceIds = allDevices.stream().map(SiteDevice::getId).toList();
        Map<Long, String> subtaskGidByDeviceId = new HashMap<>();
        for (SiteDeviceAsanaTask mapping : deviceAsanaTaskRepository.findAllById(deviceIds)) {
            subtaskGidByDeviceId.put(mapping.getDeviceId(), mapping.getSubtaskGid());
        }

        String sectionGid = asanaClient.findOrCreateSection(projectGid, SECTION_NAME);

        Map<String, JsonNode> sectionTasksByGid = new HashMap<>();
        Map<String, JsonNode> sectionTasksByName = new HashMap<>();
        for (JsonNode task : asanaClient.getSectionTasks(sectionGid)) {
            sectionTasksByGid.put(task.get("gid").asText(), task);
            sectionTasksByName.putIfAbsent(task.get("name").asText(), task);
        }

        int tasksCreated = 0, tasksUpdated = 0, subtasksCreated = 0, subtasksUpdated = 0;

        for (SiteLocation location : locations) {
            String taskGid = taskGidByLocationId.get(location.getId());
            JsonNode existingTask = taskGid != null ? sectionTasksByGid.get(taskGid) : null;
            if (existingTask == null) {
                JsonNode fallback = sectionTasksByName.get(location.getName());
                if (fallback != null) {
                    taskGid = fallback.get("gid").asText();
                    existingTask = fallback;
                }
            }
            if (existingTask == null) {
                taskGid = asanaClient.createTaskInSection(projectGid, sectionGid, location.getName());
                tasksCreated++;
            }
            if (!taskGid.equals(taskGidByLocationId.get(location.getId()))) {
                SiteLocationAsanaTask mapping = new SiteLocationAsanaTask();
                mapping.setLocationId(location.getId());
                mapping.setTaskGid(taskGid);
                locationAsanaTaskRepository.save(mapping);
            }

            Map<String, JsonNode> subtasksByGid = new HashMap<>();
            Map<String, JsonNode> subtasksByName = new HashMap<>();
            if (existingTask != null) {
                for (JsonNode subtask : asanaClient.getSubtasks(taskGid)) {
                    subtasksByGid.put(subtask.get("gid").asText(), subtask);
                    subtasksByName.putIfAbsent(subtask.get("name").asText(), subtask);
                }
            }

            List<SiteDevice> devices = devicesByLocation.getOrDefault(location.getId(), List.of());
            boolean allDevicesInService = !devices.isEmpty();

            for (SiteDevice device : devices) {
                boolean shouldBeComplete = IN_SERVICE_STATUS.equalsIgnoreCase(device.getStatus());
                if (!shouldBeComplete) allDevicesInService = false;

                String subtaskGid = subtaskGidByDeviceId.get(device.getId());
                JsonNode existingSubtask = subtaskGid != null ? subtasksByGid.get(subtaskGid) : null;
                if (existingSubtask == null) {
                    JsonNode fallback = subtasksByName.get(device.getDeviceName());
                    if (fallback != null) {
                        subtaskGid = fallback.get("gid").asText();
                        existingSubtask = fallback;
                    }
                }

                if (existingSubtask == null) {
                    subtaskGid = asanaClient.createSubtask(taskGid, device.getDeviceName(), shouldBeComplete);
                    subtasksCreated++;
                } else {
                    Map<String, Object> updates = new HashMap<>();
                    if (!device.getDeviceName().equals(existingSubtask.get("name").asText())) {
                        updates.put("name", device.getDeviceName());
                    }
                    if (existingSubtask.get("completed").asBoolean() != shouldBeComplete) {
                        updates.put("completed", shouldBeComplete);
                    }
                    if (!updates.isEmpty()) {
                        asanaClient.updateTask(subtaskGid, updates);
                        subtasksUpdated++;
                    }
                }

                if (!subtaskGid.equals(subtaskGidByDeviceId.get(device.getId()))) {
                    SiteDeviceAsanaTask mapping = new SiteDeviceAsanaTask();
                    mapping.setDeviceId(device.getId());
                    mapping.setSubtaskGid(subtaskGid);
                    deviceAsanaTaskRepository.save(mapping);
                }
            }

            Map<String, Object> taskUpdates = new HashMap<>();
            if (existingTask != null && !location.getName().equals(existingTask.get("name").asText())) {
                taskUpdates.put("name", location.getName());
            }
            boolean currentTaskCompleted = existingTask != null && existingTask.get("completed").asBoolean();
            if (!devices.isEmpty() && currentTaskCompleted != allDevicesInService) {
                taskUpdates.put("completed", allDevicesInService);
            }
            if (!taskUpdates.isEmpty()) {
                asanaClient.updateTask(taskGid, taskUpdates);
                tasksUpdated++;
            }
        }

        asanaSyncService.syncProject(projectGid);

        return new Result(tasksCreated, tasksUpdated, subtasksCreated, subtasksUpdated);
    }

    public record Result(int tasksCreated, int tasksUpdated, int subtasksCreated, int subtasksUpdated) {}
}
