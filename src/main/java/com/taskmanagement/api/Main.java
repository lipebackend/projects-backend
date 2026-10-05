package com.taskmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.api.domain.contract.repository.TaskRepository;
import com.taskmanagement.api.domain.contract.services.TaskService;
import com.taskmanagement.api.domain.service.TaskServiceImpl;
import com.taskmanagement.api.infra.repository.InMemoryTaskRepository;
import com.taskmanagement.api.infra.web.http.Server;
import com.taskmanagement.api.infra.web.http.TaskHandler;
import com.taskmanagement.api.infra.web.json.JsonMapper;

import java.io.IOException;

public class Main {
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_HOST = "0.0.0.0";

    public static void main(String[] args) throws IOException {
        String host = resolveHost();
        int port = resolvePort(args);

        TaskRepository taskRepository = new InMemoryTaskRepository();
        TaskService taskService = new TaskServiceImpl(taskRepository);
        ObjectMapper objectMapper = JsonMapper.create();
        TaskHandler taskHandler = new TaskHandler(taskService, objectMapper);

        Server server = new Server(host, port);
        server.createContext("/tasks", taskHandler);

        // Registrar shutdown hook para fechamento ordenado do servidor
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nEncerrando servidor de forma ordenada...");
            server.close();
            System.out.println("Servidor finalizado.");
        }, "shutdown-hook"));

        server.start();
        System.out.printf("Server started on %s:%d. Endpoints available at http://localhost:%d/tasks%n", host, port, port);
    }

    private static String resolveHost() {
        String envHost = System.getenv("HOST");
        if (envHost != null && !envHost.isBlank()) {
            return envHost.trim();
        }
        return DEFAULT_HOST;
    }

    public static int resolvePort(String[] args) {
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            return parsePort(args[0].trim(), "argumento de linha de comando");
        }

        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            return parsePort(envPort.trim(), "variável de ambiente PORT");
        }

        return DEFAULT_PORT;
    }

    private static int parsePort(String rawPort, String source) {
        try {
            int port = Integer.parseInt(rawPort);
            if (port < 1 || port > 65535) {
                System.err.printf("Erro de configuração: porta inválida '%s' definida via %s. A porta deve estar entre 1 e 65535.%n", rawPort, source);
                System.exit(1);
            }
            return port;
        } catch (NumberFormatException e) {
            System.err.printf("Erro de configuração: valor de porta não numérico '%s' definido via %s.%n", rawPort, source);
            System.exit(1);
            throw e; // unreachable, satisfies compiler
        }
    }
}
