package com.taskmanagement.api.infra.queue;

import com.taskmanagement.api.domain.contract.queue.TaskQueuePublisher;
import com.taskmanagement.api.domain.model.Task;

/**
 * Implementação Null Object do TaskQueuePublisher.
 * Utilizada quando o RabbitMQ está desabilitado ou indisponível em ambiente
 * local/testes.
 */
public class NoOpTaskQueuePublisher implements TaskQueuePublisher {

    @Override
    public void publishTaskCreatedEvent(Task task) {

    }

    @Override
    public void publishTaskUpdatedEvent(Task task) {

    }

    @Override
    public void publishTaskDeletedEvent(String taskId) {
        // No-op
    }
}
