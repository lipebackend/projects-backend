package infra;

import domain.Task;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * HTTP Handler managing CRUD operations for Task resources.
 * Supports standard RESTful semantics:
 * - GET    /tasks       -> 200 OK with list of tasks
 * - GET    /tasks/{id}  -> 200 OK with task or 404 Not Found
 * - POST   /tasks       -> 201 Created with created task or 400 Bad Request
 * - PUT    /tasks/{id}  -> 200 OK with updated task or 400/404
 * - DELETE /tasks/{id}  -> 204 No Content or 404 Not Found
 */
public class TaskHandler implements HttpHandler {

    private static final Pattern TITLE_FIELD_PATTERN =
            Pattern.compile("\"title\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
    private static final Pattern COMPLETED_FIELD_PATTERN =
            Pattern.compile("\"completed\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);

    private final Map<String, Task> tasks;

    public TaskHandler(Map<String, Task> tasks) {
        this.tasks = Objects.requireNonNull(tasks, "tasks repository cannot be null");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();
            String normalizedPath = path.replaceAll("^/+", "").replaceAll("/+$", "");
            String[] segments = normalizedPath.isEmpty() ? new String[0] : normalizedPath.split("/");

            if (segments.length == 0 || !segments[0].equals("tasks")) {
                sendError(exchange, 404, "Endpoint not found");
                return;
            }

            if (segments.length == 1) {
                switch (method) {
                    case "GET" -> handleGetAll(exchange);
                    case "POST" -> handleCreate(exchange);
                    default -> {
                        exchange.getResponseHeaders().set("Allow", "GET, POST");
                        sendError(exchange, 405, "Method " + method + " not allowed on /tasks");
                    }
                }
            } else if (segments.length == 2) {
                String taskId = segments[1];
                switch (method) {
                    case "GET" -> handleGetById(exchange, taskId);
                    case "PUT" -> handleUpdate(exchange, taskId);
                    case "DELETE" -> handleDelete(exchange, taskId);
                    default -> {
                        exchange.getResponseHeaders().set("Allow", "GET, PUT, DELETE");
                        sendError(exchange, 405, "Method " + method + " not allowed on /tasks/{id}");
                    }
                }
            } else {
                sendError(exchange, 404, "Resource not found");
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    // ==========================================
    // Endpoint Handlers
    // ==========================================

    private void handleGetAll(HttpExchange exchange) throws IOException {
        String json;
        synchronized (tasks) {
            json = tasks.values().stream()
                    .map(Task::toJson)
                    .collect(Collectors.joining(",", "[", "]"));
        }
        sendJsonResponse(exchange, 200, json);
    }

    private void handleGetById(HttpExchange exchange, String taskId) throws IOException {
        Task task;
        synchronized (tasks) {
            task = tasks.get(taskId);
        }

        if (task == null) {
            sendError(exchange, 404, "Task not found with id: " + taskId);
            return;
        }

        sendJsonResponse(exchange, 200, task.toJson());
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        String title = extractTitle(body);

        if (title == null || title.isBlank()) {
            sendError(exchange, 400, "Field 'title' is required and cannot be blank");
            return;
        }

        Task newTask = Task.create(title.trim());
        synchronized (tasks) {
            tasks.put(newTask.getId(), newTask);
        }

        sendJsonResponse(exchange, 201, newTask.toJson());
    }

    private void handleUpdate(HttpExchange exchange, String taskId) throws IOException {
        String body = readRequestBody(exchange);
        String newTitle = extractTitle(body);
        Boolean newCompleted = extractCompleted(body);

        if (newTitle == null && newCompleted == null) {
            sendError(exchange, 400, "Request body must contain 'title' and/or 'completed'");
            return;
        }

        if (newTitle != null && newTitle.isBlank()) {
            sendError(exchange, 400, "Field 'title' cannot be blank");
            return;
        }

        Task task;
        synchronized (tasks) {
            task = tasks.get(taskId);
            if (task == null) {
                sendError(exchange, 404, "Task not found with id: " + taskId);
                return;
            }

            if (newTitle != null) {
                task.updateTitle(newTitle.trim());
            }
            if (newCompleted != null) {
                task.setCompleted(newCompleted);
            }
        }

        sendJsonResponse(exchange, 200, task.toJson());
    }

    private void handleDelete(HttpExchange exchange, String taskId) throws IOException {
        Task removed;
        synchronized (tasks) {
            removed = tasks.remove(taskId);
        }

        if (removed == null) {
            sendError(exchange, 404, "Task not found with id: " + taskId);
            return;
        }

        sendNoContent(exchange);
    }

    // ==========================================
    // HTTP Response Helpers
    // ==========================================

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String escaped = message == null ? "Unknown error" : escapeJson(message);
        String json = String.format("{\"error\":\"%s\"}", escaped);
        sendJsonResponse(exchange, statusCode, json);
    }

    // ==========================================
    // Request Parsing & Helpers
    // ==========================================

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    private String extractTitle(String json) {
        Matcher matcher = TITLE_FIELD_PATTERN.matcher(json);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        return null;
    }

    private Boolean extractCompleted(String json) {
        Matcher matcher = COMPLETED_FIELD_PATTERN.matcher(json);
        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }
        return null;
    }

    private static String escapeJson(String raw) {
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String raw) {
        return raw.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
