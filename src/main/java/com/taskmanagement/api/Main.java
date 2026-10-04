package com.taskmanagement.api;

import com.taskmanagement.api.domain.model.Task;
import com.taskmanagement.api.infra.TaskHandler;
import com.taskmanagement.api.infra.web.http.Server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = resolvePort(args);
        String host = "localhost";
        Server server = new Server(host, port);
        Map<String, Task> tasks = new ConcurrentHashMap<>();

        server.createContext("/tasks", new TaskHandler(tasks));
        server.start();

        System.out.printf("Server started on port %d. Endpoints available at http://localhost:%d/tasks%n", port, port);
    }

    private static int resolvePort(String[] args) {
        if (args.length > 0) {
            try {
                return Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                // fall through to default port
            }
        }

        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            try {
                return Integer.parseInt(envPort);
            } catch (NumberFormatException ignored) {
                // fall through to default port
            }
        }

        return 8080;
    }
}
