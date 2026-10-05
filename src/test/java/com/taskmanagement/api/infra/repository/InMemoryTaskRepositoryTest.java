package com.taskmanagement.api.infra.repository;

import com.taskmanagement.api.domain.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTaskRepositoryTest {

    private InMemoryTaskRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTaskRepository();
    }

    @Test
    @DisplayName("Should save and retrieve task by ID")
    void shouldSaveAndRetrieveTask() {
        Task task = Task.create("Implementar repositório");
        repository.save(task);

        Optional<Task> found = repository.findById(task.getId());
        assertThat(found).isPresent().contains(task);
    }

    @Test
    @DisplayName("Should return empty optional when task does not exist")
    void shouldReturnEmptyWhenNotFound() {
        assertThat(repository.findById("inexistente")).isEmpty();
    }

    @Test
    @DisplayName("Should filter tasks by completion status")
    void shouldFilterTasksByCompletionStatus() {
        Task t1 = Task.create("Tarefa 1");
        Task t2 = Task.create("Tarefa 2").withCompleted(true);
        Task t3 = Task.create("Tarefa 3");

        repository.save(t1);
        repository.save(t2);
        repository.save(t3);

        List<Task> all = repository.findAll(null);
        List<Task> completed = repository.findAll(true);
        List<Task> pending = repository.findAll(false);

        assertThat(all).hasSize(3);
        assertThat(completed).containsExactly(t2);
        assertThat(pending).containsExactlyInAnyOrder(t1, t3);
    }

    @Test
    @DisplayName("Should update task atomically using update function")
    void shouldUpdateTaskAtomically() {
        Task task = Task.create("Título Original");
        repository.save(task);

        Optional<Task> updated = repository.update(task.getId(), existing -> existing.withTitle("Título Modificado"));

        assertThat(updated).isPresent();
        assertThat(updated.get().getTitle()).isEqualTo("Título Modificado");

        Optional<Task> found = repository.findById(task.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Título Modificado");
    }

    @Test
    @DisplayName("Should return false when deleting nonexistent task")
    void shouldReturnFalseWhenDeletingNonexistent() {
        assertThat(repository.deleteById("id-inexistente")).isFalse();
    }

    @Test
    @DisplayName("Should delete task successfully")
    void shouldDeleteTaskSuccessfully() {
        Task task = Task.create("Tarefa para deletar");
        repository.save(task);

        assertThat(repository.deleteById(task.getId())).isTrue();
        assertThat(repository.findById(task.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should support safe concurrent updates without lost updates or race conditions")
    void shouldHandleConcurrentUpdatesAtomically() throws Exception {
        Task task = Task.create("Base");
        repository.save(task);

        int threads = 30;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    repository.update(task.getId(), existing -> existing.withTitle("Update " + index));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        // Fire all threads simultaneously
        latch.countDown();

        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(3, TimeUnit.SECONDS)).isTrue();

        Optional<Task> result = repository.findById(task.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).startsWith("Update ");
    }
}
