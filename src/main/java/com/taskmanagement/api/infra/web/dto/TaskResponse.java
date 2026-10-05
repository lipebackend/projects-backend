package com.taskmanagement.api.infra.web.dto;

import com.taskmanagement.api.domain.model.Task;

import java.time.Instant;

public record TaskResponse(
        String id,
        String title,
        boolean completed,
        Instant createdAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.isCompleted(),
                task.getCreatedAt()
        );
    }
}
