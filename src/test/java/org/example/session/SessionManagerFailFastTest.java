package org.example.session;

import org.example.task.TaskRequest;
import org.example.task.TaskType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.example.session.SessionTestSupport.awaitStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionManagerFailFastTest {

    @Test
    void taskFailureSkipsRemainingTasksInThatSessionOnly() {
        SessionManager manager = new SessionManager(2);
        List<TaskRequest> requests = List.of(
                new TaskRequest(TaskType.LOG, "a", null, null),
                new TaskRequest(TaskType.HTTP, "please fail", "https://example.com", null),
                new TaskRequest(TaskType.LOG, "c", null, null),
                new TaskRequest(TaskType.SLEEP, null, null, 50L));

        String id = manager.submitSession(requests);
        SessionStatusResponse response = awaitStatus(manager, id, SessionStatus.FAILED, 2000);

        assertEquals(TaskStatus.SUCCESS, response.getTaskStatuses().get(0).getStatus());
        assertTrue(response.getTaskStatuses().get(0).getResult().isSuccess());

        assertEquals(TaskStatus.FAILED, response.getTaskStatuses().get(1).getStatus());
        assertNotNull(response.getTaskStatuses().get(1).getResult());
        assertTrue(!response.getTaskStatuses().get(1).getResult().isSuccess());

        assertEquals(TaskStatus.SKIPPED, response.getTaskStatuses().get(2).getStatus());
        assertNull(response.getTaskStatuses().get(2).getResult());

        assertEquals(TaskStatus.SKIPPED, response.getTaskStatuses().get(3).getStatus());
        assertNull(response.getTaskStatuses().get(3).getResult());

        manager.shutdown();
    }

    @Test
    void otherSessionsAreUnaffectedByAFailingSession() {
        SessionManager manager = new SessionManager(4);

        String failingId = manager.submitSession(List.of(
                new TaskRequest(TaskType.HTTP, "please fail", "https://example.com", null),
                new TaskRequest(TaskType.LOG, "never runs", null, null)));

        String succeedingId = manager.submitSession(List.of(
                new TaskRequest(TaskType.LOG, "ok1", null, null),
                new TaskRequest(TaskType.LOG, "ok2", null, null)));

        SessionStatusResponse failingResponse = awaitStatus(manager, failingId, SessionStatus.FAILED, 2000);
        SessionStatusResponse succeedingResponse = awaitStatus(manager, succeedingId, SessionStatus.COMPLETED, 2000);

        assertEquals(SessionStatus.FAILED, failingResponse.getSessionStatus());
        assertEquals(TaskStatus.SKIPPED, failingResponse.getTaskStatuses().get(1).getStatus());

        assertEquals(SessionStatus.COMPLETED, succeedingResponse.getSessionStatus());
        assertEquals(TaskStatus.SUCCESS, succeedingResponse.getTaskStatuses().get(0).getStatus());
        assertEquals(TaskStatus.SUCCESS, succeedingResponse.getTaskStatuses().get(1).getStatus());

        manager.shutdown();
    }
}
