package org.example.session;

import org.example.task.InvalidSessionRequestException;
import org.example.task.TaskFactory;
import org.example.task.TaskRequest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SessionManager {

    private static final int MIN_TASKS = 1;
    private static final int MAX_TASKS = 10;
    private static final int THREAD_POOL_SIZE = 1;

    private final ExecutorService workExecutor;
    private final ScheduledExecutorService delayScheduler;
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    public SessionManager(int poolSize) {
        this.workExecutor = Executors.newFixedThreadPool(poolSize);
        // Only fires timers and hands off to workExecutor; never runs task work itself,
        // so it never needs more than a single thread regardless of how many sleeps are pending.
        this.delayScheduler = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
    }

    public String submitSession(List<TaskRequest> requests) {
        if (requests == null || requests.size() < MIN_TASKS || requests.size() > MAX_TASKS) {
            throw new InvalidSessionRequestException(
                    "Session must contain between " + MIN_TASKS + " and " + MAX_TASKS + " tasks");
        }
        List<TaskExecution> executions = requests.stream()
                .map(req -> new TaskExecution(req, TaskFactory.createTask(req)))
                .toList();
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId, executions);
        sessions.put(sessionId, session);
        new SessionRunner(session, workExecutor, delayScheduler).start();
        return sessionId;
    }

    public SessionStatusResponse getSessionStatus(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            throw new SessionNotFoundException(sessionId);
        }
        return SessionStatusResponse.from(session);
    }

    public void shutdown() {
        workExecutor.shutdown();
        delayScheduler.shutdown();
    }
}
