package org.example.session;

import org.example.task.Task;
import org.example.task.TaskRequest;
import org.example.task.TaskResult;
import org.example.task.TaskType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.example.session.SessionTestSupport.awaitSessionStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionRunnerTest {

    private ExecutorService workExecutor;
    private ScheduledExecutorService delayScheduler;

    @BeforeEach
    void setUp() {
        workExecutor = Executors.newFixedThreadPool(2);
        delayScheduler = Executors.newScheduledThreadPool(1);
    }

    @AfterEach
    void tearDown() {
        workExecutor.shutdown();
        delayScheduler.shutdown();
    }

    @Test
    void skippedTasksNeverHaveExecuteCalled() {
        AtomicBoolean thirdTaskRan = new AtomicBoolean(false);
        AtomicBoolean fourthTaskRan = new AtomicBoolean(false);

        Task success = () -> new TaskResult("ok", true);
        Task failure = () -> new TaskResult("boom", false);
        Task spyThird = () -> {
            thirdTaskRan.set(true);
            return new TaskResult("should not run", true);
        };
        Task spyFourth = () -> {
            fourthTaskRan.set(true);
            return new TaskResult("should not run", true);
        };

        TaskExecution e1 = new TaskExecution(new TaskRequest(TaskType.LOG, "a", null, null), success);
        TaskExecution e2 = new TaskExecution(new TaskRequest(TaskType.LOG, "b", null, null), failure);
        TaskExecution e3 = new TaskExecution(new TaskRequest(TaskType.LOG, "c", null, null), spyThird);
        TaskExecution e4 = new TaskExecution(new TaskRequest(TaskType.LOG, "d", null, null), spyFourth);

        Session session = new Session("test-session", List.of(e1, e2, e3, e4));
        new SessionRunner(session, workExecutor, delayScheduler).start();
        awaitSessionStatus(session, SessionStatus.FAILED, 2000);

        assertEquals(TaskStatus.SUCCESS, e1.getStatus());
        assertEquals(TaskStatus.FAILED, e2.getStatus());
        assertEquals(TaskStatus.SKIPPED, e3.getStatus());
        assertEquals(TaskStatus.SKIPPED, e4.getStatus());

        assertFalse(thirdTaskRan.get(), "Task after a failure must never execute");
        assertFalse(fourthTaskRan.get(), "Task after a failure must never execute");
    }

    @Test
    void allTasksSucceedMarksSessionCompleted() {
        Task success = () -> new TaskResult("ok", true);
        TaskExecution e1 = new TaskExecution(new TaskRequest(TaskType.LOG, "a", null, null), success);
        TaskExecution e2 = new TaskExecution(new TaskRequest(TaskType.LOG, "b", null, null), success);

        Session session = new Session("test-session-2", List.of(e1, e2));
        new SessionRunner(session, workExecutor, delayScheduler).start();
        awaitSessionStatus(session, SessionStatus.COMPLETED, 2000);

        assertTrue(session.getExecutions().stream().allMatch(e -> e.getStatus() == TaskStatus.SUCCESS));
    }

    @Test
    void tasksRunInSubmittedOrderEvenAcrossMultipleWorkerThreads() {
        StringBuilder order = new StringBuilder();
        Task a = () -> {
            order.append('A');
            return new TaskResult("ok", true);
        };
        Task b = () -> {
            order.append('B');
            return new TaskResult("ok", true);
        };
        Task c = () -> {
            order.append('C');
            return new TaskResult("ok", true);
        };

        TaskExecution e1 = new TaskExecution(new TaskRequest(TaskType.LOG, "a", null, null), a);
        TaskExecution e2 = new TaskExecution(new TaskRequest(TaskType.LOG, "b", null, null), b);
        TaskExecution e3 = new TaskExecution(new TaskRequest(TaskType.LOG, "c", null, null), c);

        Session session = new Session("test-session-3", List.of(e1, e2, e3));
        new SessionRunner(session, workExecutor, delayScheduler).start();
        awaitSessionStatus(session, SessionStatus.COMPLETED, 2000);

        assertEquals("ABC", order.toString());
    }
}
