package com.taskmanagement.api.infra.web.http;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private static final int DEFAULT_BACKLOG = 50;
    private static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 5;


    private final HttpServer httpServer;
    private final ExecutorService executor;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public Server(String host, int port) throws IOException {
        validatePort(port);
        validateHost(host);

        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
        this.httpServer.setExecutor(this.executor);

    }

    private static void validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(String.format("Invalid port %d", port));
        }
    }

    private static void validateHost(String host) {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException(String.format("Invalid host %s", host));
        }
    }

    public synchronized void start() {
        if (isRunning.compareAndSet(false, true)) {
            httpServer.start();
            LOGGER.log(Level.INFO, "Servidor iniciado em {0}:{1}"
            );
        } else {
            LOGGER.log(Level.WARNING, "Tentativa de iniciar servidor já em execução.");
        }
    }

    public synchronized void stop(int delaySeconds) {
        if (!isRunning.compareAndSet(true, false)) {
            return;
        }

        LOGGER.log(Level.INFO, "Encerrando servidor...");
        httpServer.stop(Math.max(0, delaySeconds));

        // Encerramento ordenado do pool de threads
        executor.shutdown();
        try {
            if (!executor.awaitTermination(delaySeconds, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.log(Level.INFO, "Servidor finalizado com sucesso.");
    }


    public void stop() {
        stop(DEFAULT_SHUTDOWN_TIMEOUT_SECONDS);
    }

    @Override
    public void close() {
        stop(DEFAULT_SHUTDOWN_TIMEOUT_SECONDS);
    }

    public void createContext(String path, HttpHandler handler) {
        httpServer.createContext(path, handler);
    }
}
