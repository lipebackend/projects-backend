package com.taskmanagement.api.infra.repository;

import com.taskmanagement.api.domain.contract.repository.TaskRepository;
import com.taskmanagement.api.domain.model.Task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import javax.sql.DataSource;

public class PostgresTaskRepository implements TaskRepository, AutoCloseable {
    private final BlockingQueue<Task> batchQueue = new LinkedBlockingQueue<>();
    private DataSource dataSource;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public PostgresTaskRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        this.scheduler.scheduleAtFixedRate(this::triggerFlush, 30, 30, TimeUnit.SECONDS);
    }

    private void flushBatch(List<Task> batch) {
    String sql = "INSERT INTO tasks (id, title, completed, created_at) VALUES (?, ?, ?, ?)";
    
    try (Connection conn = this.dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        for (Task task : batch) {
            stmt.setString(1, task.getId());
            stmt.setString(2, task.getTitle());
            stmt.setBoolean(3, task.isCompleted());
            stmt.setTimestamp(4, java.sql.Timestamp.from(task.getCreatedAt()));
            stmt.addBatch();
        }
        
        stmt.executeBatch();
    } catch (SQLException e) {
        throw new RuntimeException("Falha ao processar lote de tasks no Postgres", e);
    }
}

    public List<Task> findAll() {
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS tasks (
                    id VARCHAR(36) PRIMARY KEY,
                    title TEXT NOT NULL,
                    completed BOOLEAN NOT NULL,
                    created_at TIMESTAMP NOT NULL
                );
                """;
        String selectSql = "SELECT id, title, completed, created_at FROM tasks";
        List<Task> tasks = new ArrayList<>();

        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement createStmt = conn.prepareStatement(createTableSql)) {
            createStmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao criar tabela tasks no Postgres", e);
        }

        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql);
             ResultSet resultSet = stmt.executeQuery()) {
            while (resultSet.next()) {
                tasks.add(new Task(
                        resultSet.getString("id"),
                        resultSet.getString("title"),
                        resultSet.getBoolean("completed"),
                        resultSet.getTimestamp("created_at").toInstant()));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao buscar tasks no Postgres", e);
        }

        return tasks;
    }
    
    @Override
    public List<Task> findAll(Boolean completedFilter) {
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS tasks (
                    id VARCHAR(36) PRIMARY KEY,
                    title TEXT NOT NULL,
                    completed BOOLEAN NOT NULL,
                    created_at TIMESTAMP NOT NULL
                );
                """;
        String selectSql = "SELECT id, title, completed, created_at FROM tasks";
        List<Task> tasks = new ArrayList<>();

        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement createStmt = conn.prepareStatement(createTableSql)) {
            createStmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao criar tabela tasks no Postgres", e);
        }

        if (completedFilter != null) {
            selectSql += " WHERE completed = ?";
        }

        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            if (completedFilter != null) {
                stmt.setBoolean(1, completedFilter);
            }
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    tasks.add(new Task(
                            resultSet.getString("id"),
                            resultSet.getString("title"),
                            resultSet.getBoolean("completed"),
                            resultSet.getTimestamp("created_at").toInstant()));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao buscar tasks no Postgres", e);
        }

        return tasks;
    }

    @Override
    public Optional<Task> findById(String id) {
        String sql = "SELECT id, title, completed, created_at FROM tasks WHERE id = ?";
        try (Connection conn = this.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new Task(
                            resultSet.getString("id"),
                            resultSet.getString("title"),
                            resultSet.getBoolean("completed"),
                            resultSet.getTimestamp("created_at").toInstant()));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao buscar task por ID no Postgres", e);
        }
        return Optional.empty();
    }

    private void triggerFlush() {
        List<Task> batch = new ArrayList<>(100);
        batchQueue.drainTo(batch, 100);
        if (!batch.isEmpty()) {
            flushBatch(batch);
        }
    }

    @Override
    public Task save(Task task) {
        batchQueue.add(task);
        if (batchQueue.size() >= 100) {
            triggerFlush();
        }
        return task;
    }

    @Override
    public Optional<Task> update(String id, UnaryOperator<Task> updateFunction) {
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public boolean deleteById(String id) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteById'");
    }

    @Override
    public boolean existsById(String id) {
        throw new UnsupportedOperationException("Unimplemented method 'existsById'");
    }

    @Override
    public void close() throws Exception {
        triggerFlush();
        scheduler.shutdown();
    }


    private void initTable() {
    String sql = """
        CREATE TABLE IF NOT EXISTS tasks (
            id VARCHAR(36) PRIMARY KEY,
            title VARCHAR(255) NOT NULL,
            completed BOOLEAN NOT NULL DEFAULT FALSE,
            created_at TIMESTAMP WITH TIME ZONE NOT NULL
        )
        """;
    try (Connection conn = this.dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.execute();
    } catch (SQLException e) {
        throw new RuntimeException("Falha ao inicializar tabela tasks no Postgres", e);
    }


}
}
