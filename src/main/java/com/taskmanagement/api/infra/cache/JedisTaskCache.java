package com.taskmanagement.api.infra.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.api.domain.contract.cache.TaskCache;
import com.taskmanagement.api.domain.model.Task;
import redis.clients.jedis.Jedis;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * @author wroc
 */
public class JedisTaskCache implements TaskCache {

    private static final String KEY_PREFIX = "task:";

    private final Jedis jedis;
    private final ObjectMapper objectMapper;

    public JedisTaskCache(Jedis jedis, ObjectMapper objectMapper) {
        this.jedis = Objects.requireNonNull(jedis, "jedis cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    @Override
    public Optional<Task> get(String id) {
        String json = jedis.get(keyFor(id));
        if (json == null) {
            return Optional.empty();
        }

        try {
            CachedTask cachedTask = objectMapper.readValue(json, CachedTask.class);
            return Optional.of(cachedTask.toTask());
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not deserialize task from Redis for id: " + id, exception);
        }
    }

    @Override
    public void put(Task task) {
        Objects.requireNonNull(task, "task cannot be null");

        try {
            String json = objectMapper.writeValueAsString(CachedTask.from(task));
            jedis.set(keyFor(task.getId()), json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize task for Redis: " + task.getId(), exception);
        }
    }

    @Override
    public void evict(String id) {
        jedis.del(keyFor(id));
    }

    private static String keyFor(String id) {
        Objects.requireNonNull(id, "id cannot be null");
        return KEY_PREFIX + id;
    }

    private record CachedTask(String id, String title, boolean completed, Instant createdAt) {

        private static CachedTask from(Task task) {
            return new CachedTask(task.getId(), task.getTitle(), task.isCompleted(), task.getCreatedAt());
        }

        private Task toTask() {
            return new Task(id, title, completed, createdAt);
        }
    }
}
