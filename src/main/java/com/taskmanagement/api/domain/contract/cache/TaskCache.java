package com.taskmanagement.api.domain.contract.cache;

import com.taskmanagement.api.domain.model.Task;
import java.util.Optional;

public interface TaskCache {

    Optional<Task> get(String id);

    void put(Task task);

    void evict(String id);
}
