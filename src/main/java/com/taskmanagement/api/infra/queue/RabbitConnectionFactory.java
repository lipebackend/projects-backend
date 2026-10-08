package com.taskmanagement.api.infra.queue;

public final class  RabbitConnectionFactory {
    private RabbitConnectionFactory() {}

    public static RabbitConnectionBuilder builder() {
        return new RabbitConnectionBuilder();
    }

}