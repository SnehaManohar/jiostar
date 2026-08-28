package org.example.session;

import org.example.task.TaskResult;
import org.example.task.TaskType;

import java.util.ArrayList;
import java.util.List;

public class SessionStatusResponse {

    private final String sessionId;
    private final SessionStatus sessionStatus;
    private final List<TaskStatusView> taskStatuses;

    private SessionStatusResponse(String sessionId, SessionStatus sessionStatus, List<TaskStatusView> taskStatuses) {
        this.sessionId = sessionId;
        this.sessionStatus = sessionStatus;
        this.taskStatuses = taskStatuses;
    }

    public static SessionStatusResponse from(Session session) {
        List<TaskStatusView> views = new ArrayList<>();
        List<TaskExecution> executions = session.getExecutions();
        for (int i = 0; i < executions.size(); i++) {
            TaskExecution exec = executions.get(i);
            views.add(new TaskStatusView(
                    i,
                    exec.getRequest().getType(),
                    exec.getStatus(),
                    exec.getResult()));
        }
        return new SessionStatusResponse(session.getId(), session.getStatus(), List.copyOf(views));
    }

    public String getSessionId() {
        return sessionId;
    }

    public SessionStatus getSessionStatus() {
        return sessionStatus;
    }

    public List<TaskStatusView> getTaskStatuses() {
        return taskStatuses;
    }

    public static class TaskStatusView {
        private final int index;
        private final TaskType type;
        private final TaskStatus status;
        private final TaskResult result;

        public TaskStatusView(int index, TaskType type, TaskStatus status, TaskResult result) {
            this.index = index;
            this.type = type;
            this.status = status;
            this.result = result;
        }

        public int getIndex() {
            return index;
        }

        public TaskType getType() {
            return type;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public TaskResult getResult() {
            return result;
        }
    }
}
