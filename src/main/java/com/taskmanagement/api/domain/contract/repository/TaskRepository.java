package com.taskmanagement.api.domain.contract.repository;

import com.taskmanagement.api.domain.model.Task;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface TaskRepository {
    List<Task> findAll(Boolean completedFilter);

    Optional<Task> findById(String id);

    Task save(Task task);

    Optional<Task> update(String id, UnaryOperator<Task> updateFunction);

    boolean deleteById(String id);

    boolean existsById(String id);
}
