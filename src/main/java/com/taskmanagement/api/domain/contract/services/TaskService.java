package com.taskmanagement.api.domain.contract.services;

import com.taskmanagement.api.domain.model.Task;

import java.util.List;

public interface TaskService {
    List<Task> getAllTasks(Boolean completedFilter);

    Task getTaskById(String id);

    Task createTask(String title);

    Task replaceTask(String id, String title, boolean completed);

    Task patchTask(String id, String title, Boolean completed);

    void deleteTask(String id);
}
