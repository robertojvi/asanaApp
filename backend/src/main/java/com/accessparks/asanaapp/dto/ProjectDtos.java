package com.accessparks.asanaapp.dto;

import jakarta.validation.constraints.NotBlank;

public class ProjectDtos {

    public record CreateBlankProjectRequest(@NotBlank String name) {}

    public record DuplicateProjectRequest(
        @NotBlank String templateGid,
        @NotBlank String name
    ) {}

    public record UpdateTaskDateRequest(
        String dueOn, // "YYYY-MM-DD" or null to clear
        String expectedDueDateFieldGid, // custom field gid, if updating that field
        String expectedDueDate // "YYYY-MM-DD" or null
    ) {}
}
