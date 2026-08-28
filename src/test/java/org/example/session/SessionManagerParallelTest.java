package org.example.session;

import org.example.task.TaskRequest;
import org.example.task.TaskType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.example.session.SessionTestSupport.awaitStatus;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionManagerParallelTest {

    @Test
    void independentSessionsRunConcurrentlyNotSequentially() {
        SessionManager manager = new SessionManager(3);
        long sleepMillis = 300;

        long start = System.currentTimeMillis();
        List<String> ids = List.of(
                manager.submitSession(List.of(new TaskRequest(TaskType.SLEEP, null, null, sleepMillis))),
                manager.submitSession(List.of(new TaskRequest(TaskType.SLEEP, null, null, sleepMillis))),
                manager.submitSession(List.of(new TaskRequest(TaskType.SLEEP, null, null, sleepMillis))));

        for (String id : ids) {
            awaitStatus(manager, id, SessionStatus.COMPLETED, 2000);
        }
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < sleepMillis * ids.size(),
                "Expected concurrent execution to finish in well under " + (sleepMillis * ids.size())
                        + "ms sequential time, took " + elapsed + "ms");

        manager.shutdown();
    }

    @Test
    void sleepingSessionsDoNotOccupyWorkerThreadsEvenWithASinglePoolThread() {
        // Pool size 1: if SLEEP blocked a worker thread, these 5 sessions would
        // serialize to 5 * sleepMillis. Since the wait is realized via a timer
        // instead of Thread.sleep on a pool thread, they should all finish in
        // roughly one sleepMillis interval regardless of pool size.
        SessionManager manager = new SessionManager(1);
        long sleepMillis = 200;
        int sessionCount = 5;

        long start = System.currentTimeMillis();
        List<String> ids = java.util.stream.IntStream.range(0, sessionCount)
                .mapToObj(i -> manager.submitSession(List.of(new TaskRequest(TaskType.SLEEP, null, null, sleepMillis))))
                .toList();

        for (String id : ids) {
            awaitStatus(manager, id, SessionStatus.COMPLETED, 3000);
        }
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < sleepMillis * sessionCount,
                "Expected sleeping sessions to not serialize on a single worker thread; expected well under "
                        + (sleepMillis * sessionCount) + "ms, took " + elapsed + "ms");

        manager.shutdown();
    }
}
