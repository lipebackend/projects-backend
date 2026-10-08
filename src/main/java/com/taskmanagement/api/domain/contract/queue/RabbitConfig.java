package com.taskmanagement.api.domain.contract.queue;

public record RabbitConfig(
        String host,
        int port,
        String username,
        String password,
        int connectionTimeoutMs) {
    public RabbitConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("RabbitMQ host não pode ser vazio");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Porta inválida para RabbitMQ: " + port);
        }
    }
}
