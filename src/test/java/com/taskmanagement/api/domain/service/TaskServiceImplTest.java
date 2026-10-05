package com.taskmanagement.api.domain.service;

import com.taskmanagement.api.domain.contract.repository.TaskRepository;
import com.taskmanagement.api.domain.exception.TaskNotFoundException;
import com.taskmanagement.api.domain.exception.ValidationException;
import com.taskmanagement.api.domain.model.Task;
import com.taskmanagement.api.infra.repository.InMemoryTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskServiceImplTest {

    private TaskRepository repository;
    private TaskServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTaskRepository();
        service = new TaskServiceImpl(repository);
    }

    @Test
    @DisplayName("Should create task successfully when title is valid")
    void shouldCreateTaskSuccessfully() {
        Task task = service.createTask("Estudar Clean Architecture");

        assertThat(task.getId()).isNotBlank();
        assertThat(task.getTitle()).isEqualTo("Estudar Clean Architecture");
        assertThat(task.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("Should throw ValidationException when creating task with blank title")
    void shouldThrowValidationExceptionWhenTitleBlank() {
        assertThatThrownBy(() -> service.createTask("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Field 'title' is required and cannot be blank");
    }

    @Test
    @DisplayName("Should retrieve task by id or throw TaskNotFoundException")
    void shouldGetByIdOrThrowNotFound() {
        Task task = service.createTask("Tarefa de teste");

        Task retrieved = service.getTaskById(task.getId());
        assertThat(retrieved).isEqualTo(task);

        assertThatThrownBy(() -> service.getTaskById("id-inexistente"))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("Task not found with id: id-inexistente");
    }

    @Test
    @DisplayName("Should replace task with PUT semantics (title and completed)")
    void shouldReplaceTaskSuccessfully() {
        Task task = service.createTask("Título inicial");

        Task replaced = service.replaceTask(task.getId(), "Título atualizado", true);
        assertThat(replaced.getTitle()).isEqualTo("Título atualizado");
        assertThat(replaced.isCompleted()).isTrue();

        Task fromDb = service.getTaskById(task.getId());
        assertThat(fromDb.getTitle()).isEqualTo("Título atualizado");
        assertThat(fromDb.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("Should throw TaskNotFoundException when replacing nonexistent task")
    void shouldThrowWhenReplacingNonexistent() {
        assertThatThrownBy(() -> service.replaceTask("id-nao-existe", "Novo Título", true))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    @DisplayName("Should patch task with PATCH semantics (partial title or completed)")
    void shouldPatchTaskSelectively() {
        Task task = service.createTask("Título Inicial");

        // Patch only completed
        Task patchedCompleted = service.patchTask(task.getId(), null, true);
        assertThat(patchedCompleted.getTitle()).isEqualTo("Título Inicial");
        assertThat(patchedCompleted.isCompleted()).isTrue();

        // Patch only title
        Task patchedTitle = service.patchTask(task.getId(), "Título Novo", null);
        assertThat(patchedTitle.getTitle()).isEqualTo("Título Novo");
        assertThat(patchedTitle.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("Should throw ValidationException when patch has neither title nor completed")
    void shouldThrowWhenPatchIsEmpty() {
        Task task = service.createTask("Título");

        assertThatThrownBy(() -> service.patchTask(task.getId(), null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Request body must contain 'title' and/or 'completed'");
    }

    @Test
    @DisplayName("Should delete task successfully or throw TaskNotFoundException")
    void shouldDeleteTaskOrThrow() {
        Task task = service.createTask("Para Deletar");

        service.deleteTask(task.getId());

        assertThatThrownBy(() -> service.getTaskById(task.getId()))
                .isInstanceOf(TaskNotFoundException.class);

        assertThatThrownBy(() -> service.deleteTask(task.getId()))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    @DisplayName("Should filter tasks by completion in getAllTasks")
    void shouldFilterTasksInGetAll() {
        service.createTask("T1");
        Task t2 = service.createTask("T2");
        service.replaceTask(t2.getId(), "T2", true);

        List<Task> pending = service.getAllTasks(false);
        List<Task> completed = service.getAllTasks(true);

        assertThat(pending).hasSize(1).extracting(Task::getTitle).containsExactly("T1");
        assertThat(completed).hasSize(1).extracting(Task::getTitle).containsExactly("T2");
    }
}
