package domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Rich Domain Entity representing a Task in the system.
 * Encapsulates identity, state transitions, validation invariants, and business behaviors.
 */
public class Task {
    private final String id;
    private String title;
    private boolean completed;
    private final Instant createdAt;

    /**
     * Factory method for creating a brand-new Task (server-assigned ID, default pending status, current timestamp).
     *
     * @param title the title of the task
     * @return a new Task instance
     */
    public static Task create(String title) {
        return new Task(UUID.randomUUID().toString(), title, false, Instant.now());
    }

    /**
     * Full reconstitution constructor (used when deserializing or restoring existing state).
     *
     * @param id        the unique identifier
     * @param title     the task description/title
     * @param completed the completion status
     * @param createdAt the creation instant
     */
    public Task(String id, String title, boolean completed, Instant createdAt) {
        this.id = validateId(id);
        this.title = validateTitle(title);
        this.completed = completed;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    /**
     * Convenience constructor when an ID is explicitly assigned to a new pending task.
     *
     * @param id    the unique identifier
     * @param title the task title
     */
    public Task(String id, String title) {
        this(id, title, false, Instant.now());
    }

    // ==========================================
    // Invariants & Validation
    // ==========================================

    private static String validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Task ID cannot be null or blank");
        }
        return id.strip();
    }

    private static String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank");
        }
        return title.strip();
    }

    // ==========================================
    // Domain Behaviors (Intention-Revealing Operations)
    // ==========================================

    /**
     * Marks the task as completed.
     */
    public void complete() {
        this.completed = true;
    }

    /**
     * Reopens a completed task.
     */
    public void reopen() {
        this.completed = false;
    }

    /**
     * Updates the title with invariant enforcement.
     *
     * @param newTitle the updated title
     */
    public void updateTitle(String newTitle) {
        this.title = validateTitle(newTitle);
    }

    // Compatibility setters if required by frameworks
    public void setCompleted(boolean completed) {
        if (completed) {
            complete();
        } else {
            reopen();
        }
    }

    public void setTitle(String title) {
        updateTitle(title);
    }

    // ==========================================
    // Getters / Queries
    // ==========================================

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // ==========================================
    // JSON Serialization Helper
    // ==========================================

    /**
     * Serializes this task into standard JSON conforming to the API specification.
     */
    public String toJson() {
        return String.format(
                "{\"id\":\"%s\",\"title\":\"%s\",\"completed\":%b,\"createdAt\":\"%s\"}",
                escapeJson(id),
                escapeJson(title),
                completed,
                createdAt.toString()
        );
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

    // ==========================================
    // Identity & Object Contract
    // ==========================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", completed=" + completed +
                ", createdAt=" + createdAt +
                '}';
    }
}
