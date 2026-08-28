package org.example.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskFactoryTest {

    @Test
    void createsLogTask() {
        Task task = TaskFactory.createTask(new TaskRequest(TaskType.LOG, "hi", null, null));
        assertInstanceOf(LogTask.class, task);
    }

    @Test
    void createsHttpTask() {
        Task task = TaskFactory.createTask(new TaskRequest(TaskType.HTTP, "hi", "https://example.com", null));
        assertInstanceOf(HttpTask.class, task);
    }

    @Test
    void createsSleepTask() {
        Task task = TaskFactory.createTask(new TaskRequest(TaskType.SLEEP, null, null, 10L));
        assertInstanceOf(SleepTask.class, task);
    }

    @Test
    void httpTaskMissingUrlThrows() {
        TaskRequest request = new TaskRequest(TaskType.HTTP, "hi", null, null);
        assertThrows(InvalidSessionRequestException.class, () -> TaskFactory.createTask(request));
    }

    @Test
    void logTaskMissingMessageThrows() {
        TaskRequest request = new TaskRequest(TaskType.LOG, null, null, null);
        assertThrows(InvalidSessionRequestException.class, () -> TaskFactory.createTask(request));
    }

    @Test
    void sleepTaskMissingDurationThrows() {
        TaskRequest request = new TaskRequest(TaskType.SLEEP, null, null, null);
        assertThrows(InvalidSessionRequestException.class, () -> TaskFactory.createTask(request));
    }
}
