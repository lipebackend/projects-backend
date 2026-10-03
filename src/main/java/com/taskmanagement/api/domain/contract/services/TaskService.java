package com.taskmanagement.api.domain.contract.services;

import java.util.List;

import com.taskmanagement.api.domain.model.Task;

public interface TaskService {
    List<Task> getAllTasks();
    Task getTaskById(String id);
    Task createTask(String title);
    Task updateTaskTitle(String id, String title);
    Task updateTaskCompleted(String id, boolean completed);
    void deleteTask(String id);
}
