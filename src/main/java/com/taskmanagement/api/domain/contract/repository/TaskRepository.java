package com.taskmanagement.api.domain.contract.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.taskmanagement.api.domain.model.Task;

public interface TaskRepository {
    Collection<Task> findAll();
    Task findById(UUID id);
    Task findByName(String name);
    Collection<Task> findAllByName(String name);
    Collection<Task> findAllByStatus(String status);
    Collection<Task> findAllByStatus(String status, UUID id);
}
