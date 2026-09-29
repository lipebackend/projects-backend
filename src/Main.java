import domain.Task;
import infra.Server;
import infra.TaskHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        Server server = new Server(port);
        Map<String, Task> tasks = new ConcurrentHashMap<>();

        server.createContext("/tasks", new TaskHandler(tasks));
        server.start();

        System.out.printf("Server started on port %d. Endpoints available at http://localhost:%d/tasks%n", port, port);
    }
}
