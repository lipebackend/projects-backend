package com.taskmanagement.api.domain.service;

import com.taskmanagement.api.domain.contract.repository.TaskRepository;
import com.taskmanagement.api.domain.contract.services.TaskService;
import com.taskmanagement.api.domain.exception.TaskNotFoundException;
import com.taskmanagement.api.domain.exception.ValidationException;
import com.taskmanagement.api.domain.model.Task;

import java.util.List;
import java.util.Objects;

public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository cannot be null");
    }

    @Override
    public List<Task> getAllTasks(Boolean completedFilter) {
        return taskRepository.findAll(completedFilter);
    }

    @Override
    public Task getTaskById(String id) {
        String validatedId = validateId(id);
        return taskRepository.findById(validatedId)
                .orElseThrow(() -> new TaskNotFoundException(validatedId));
    }

    @Override
    public Task createTask(String title) {
        String validatedTitle = validateTitle(title);
        Task task = Task.create(validatedTitle);
        return taskRepository.save(task);
    }

    @Override
    public Task replaceTask(String id, String title, boolean completed) {
        String validatedId = validateId(id);
        String validatedTitle = validateTitle(title);

        return taskRepository.update(validatedId, existing -> existing.withTitleAndCompleted(validatedTitle, completed))
                .orElseThrow(() -> new TaskNotFoundException(validatedId));
    }

    @Override
    public Task patchTask(String id, String title, Boolean completed) {
        String validatedId = validateId(id);

        if (title == null && completed == null) {
            throw new ValidationException("Request body must contain 'title' and/or 'completed'");
        }

        String validatedTitle = null;
        if (title != null) {
            validatedTitle = validateTitle(title);
        }

        final String finalTitle = validatedTitle;
        return taskRepository.update(validatedId, existing -> {
            Task updated = existing;
            if (finalTitle != null) {
                updated = updated.withTitle(finalTitle);
            }
            if (completed != null) {
                updated = updated.withCompleted(completed);
            }
            return updated;
        }).orElseThrow(() -> new TaskNotFoundException(validatedId));
    }

    @Override
    public void deleteTask(String id) {
        String validatedId = validateId(id);
        boolean deleted = taskRepository.deleteById(validatedId);
        if (!deleted) {
            throw new TaskNotFoundException(validatedId);
        }
    }

    private static String validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("Task ID cannot be null or blank");
        }
        return id.strip();
    }

    private static String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Field 'title' is required and cannot be blank");
        }
        return title.strip();
    }
}
