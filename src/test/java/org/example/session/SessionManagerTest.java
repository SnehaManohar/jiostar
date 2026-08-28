package org.example.session;

import org.example.task.InvalidSessionRequestException;
import org.example.task.TaskRequest;
import org.example.task.TaskType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.example.session.SessionTestSupport.awaitStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionManagerTest {

    @Test
    void submittingOneTaskSucceeds() {
        SessionManager manager = new SessionManager(2);
        String id = manager.submitSession(List.of(new TaskRequest(TaskType.LOG, "hi", null, null)));
        awaitStatus(manager, id, SessionStatus.COMPLETED, 1000);
        manager.shutdown();
    }

    @Test
    void submittingTenTasksSucceeds() {
        SessionManager manager = new SessionManager(2);
        List<TaskRequest> requests = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> new TaskRequest(TaskType.LOG, "msg" + i, null, null))
                .toList();
        String id = manager.submitSession(requests);
        SessionStatusResponse response = awaitStatus(manager, id, SessionStatus.COMPLETED, 1000);
        assertEquals(10, response.getTaskStatuses().size());
        manager.shutdown();
    }

    @Test
    void submittingZeroTasksRejected() {
        SessionManager manager = new SessionManager(2);
        assertThrows(InvalidSessionRequestException.class, () -> manager.submitSession(List.of()));
        manager.shutdown();
    }

    @Test
    void submittingElevenTasksRejected() {
        SessionManager manager = new SessionManager(2);
        List<TaskRequest> requests = java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> new TaskRequest(TaskType.LOG, "msg" + i, null, null))
                .toList();
        assertThrows(InvalidSessionRequestException.class, () -> manager.submitSession(requests));
        manager.shutdown();
    }

    @Test
    void unknownSessionIdThrows() {
        SessionManager manager = new SessionManager(2);
        assertThrows(SessionNotFoundException.class, () -> manager.getSessionStatus("does-not-exist"));
        manager.shutdown();
    }

    @Test
    void taskOrderIsPreservedInStatusResponse() {
        SessionManager manager = new SessionManager(1);
        List<TaskRequest> requests = List.of(
                new TaskRequest(TaskType.LOG, "first", null, null),
                new TaskRequest(TaskType.LOG, "second", null, null),
                new TaskRequest(TaskType.LOG, "third", null, null));
        String id = manager.submitSession(requests);
        SessionStatusResponse response = awaitStatus(manager, id, SessionStatus.COMPLETED, 1000);

        assertEquals("first", requests.get(response.getTaskStatuses().get(0).getIndex()).getMessage());
        assertEquals(TaskType.LOG, response.getTaskStatuses().get(0).getType());
        assertEquals("Logged task completed", response.getTaskStatuses().get(0).getResult().getMessage());
        assertEquals(0, response.getTaskStatuses().get(0).getIndex());
        assertEquals(1, response.getTaskStatuses().get(1).getIndex());
        assertEquals(2, response.getTaskStatuses().get(2).getIndex());
        manager.shutdown();
    }
}
