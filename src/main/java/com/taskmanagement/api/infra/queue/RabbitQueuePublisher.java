package com.taskmanagement.api.infra.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.taskmanagement.api.domain.contract.queue.TaskQueuePublisher;
import com.taskmanagement.api.domain.model.Task;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RabbitQueuePublisher implements TaskQueuePublisher, AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(RabbitQueuePublisher.class.getName());

    public static final String TASK_CREATED_QUEUE = "task.created";
    public static final String TASK_UPDATED_QUEUE = "task.updated";
    public static final String TASK_DELETED_QUEUE = "task.deleted";

    private final Connection connection;
    private final ObjectMapper objectMapper;

    public RabbitQueuePublisher(Connection connection, ObjectMapper objectMapper) {
        this.connection = Objects.requireNonNull(connection, "connection não pode ser nula");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper não pode ser nulo");
        declareTopology();
    }

    private void declareTopology() {
        try (Channel channel = connection.createChannel()) {
            channel.queueDeclare(TASK_CREATED_QUEUE, true, false, false, null);
            channel.queueDeclare(TASK_UPDATED_QUEUE, true, false, false, null);
            channel.queueDeclare(TASK_DELETED_QUEUE, true, false, false, null);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Não foi possível declarar a topologia inicial das filas no RabbitMQ", exception);
        }
    }

    @Override
    public void publishTaskCreatedEvent(Task task) {
        publish(TASK_CREATED_QUEUE, Objects.requireNonNull(task, "task não pode ser nula"));
    }

    @Override
    public void publishTaskUpdatedEvent(Task task) {
        publish(TASK_UPDATED_QUEUE, Objects.requireNonNull(task, "task não pode ser nula"));
    }

    @Override
    public void publishTaskDeletedEvent(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId não pode ser nulo ou vazio");
        }
        publish(TASK_DELETED_QUEUE, Map.of("id", taskId));
    }

    private void publish(String queue, Object payload) {
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Erro ao serializar evento para a fila " + queue, exception);
        }

        try (Channel channel = connection.createChannel()) {
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .contentEncoding(StandardCharsets.UTF_8.name())
                    .deliveryMode(2)
                    .type(queue)
                    .build();

            channel.basicPublish("", queue, props, body);
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao publicar evento na fila RabbitMQ: " + queue, exception);
        }
    }

    @Override
    public void close() {
        try {
            if (connection.isOpen()) {
                connection.close();
            }
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Falha ao fechar conexão com RabbitMQ de forma limpa", exception);
        }
    }
}
