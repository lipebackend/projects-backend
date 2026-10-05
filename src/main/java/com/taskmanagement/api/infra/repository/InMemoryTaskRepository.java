package com.taskmanagement.api.infra.repository;

import com.taskmanagement.api.domain.contract.repository.TaskRepository;
import com.taskmanagement.api.domain.model.Task;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;

/**
 * Thread-safe in-memory repository implementation using ConcurrentHashMap.
 * Concurrency protection is achieved by immutable Task entities combined with
 * atomic bucket-level operations (computeIfPresent).
 */
public class InMemoryTaskRepository implements TaskRepository {

    private final ConcurrentMap<String, Task> storage;

    public InMemoryTaskRepository() {
        this.storage = new ConcurrentHashMap<>();
    }

    public InMemoryTaskRepository(ConcurrentMap<String, Task> initialStorage) {
        this.storage = Objects.requireNonNull(initialStorage, "storage cannot be null");
    }

    @Override
    public List<Task> findAll(Boolean completedFilter) {
        return storage.values().stream()
                .filter(task -> completedFilter == null || task.isCompleted() == completedFilter)
                .toList();
    }

    @Override
    public Optional<Task> findById(String id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Task save(Task task) {
        Objects.requireNonNull(task, "Task cannot be null");
        storage.put(task.getId(), task);
        return task;
    }

    @Override
    public Optional<Task> update(String id, UnaryOperator<Task> updateFunction) {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(updateFunction, "updateFunction cannot be null");

        Task updated = storage.computeIfPresent(id, (key, existing) -> {
            Task result = updateFunction.apply(existing);
            return Objects.requireNonNull(result, "Updated task cannot be null");
        });

        return Optional.ofNullable(updated);
    }

    @Override
    public boolean deleteById(String id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return storage.remove(id) != null;
    }

    @Override
    public boolean existsById(String id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return storage.containsKey(id);
    }
}
