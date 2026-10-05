package com.taskmanagement.api.domain.exception;

public class TaskNotFoundException extends RuntimeException {
    private final String taskId;

    public TaskNotFoundException(String taskId) {
        super("Task not found with id: " + taskId);
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }
}
