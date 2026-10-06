package com.taskmanagement.api.domain.contract.queue;

import com.taskmanagement.api.domain.model.Task;

public interface TaskQueuePublisher {

    void publishTaskCreatedEvent(Task task);

    void publishTaskUpdatedEvent(Task task);

    void publishTaskDeletedEvent(String taskId);
}
