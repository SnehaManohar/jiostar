package org.example.session;

import java.time.Instant;
import java.util.List;

public class Session {

    private final String id;
    private final List<TaskExecution> executions;
    private volatile SessionStatus status = SessionStatus.PENDING;
    private final Instant createdAt;

    public Session(String id, List<TaskExecution> executions) {
        this.id = id;
        this.executions = List.copyOf(executions);
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public List<TaskExecution> getExecutions() {
        return executions;
    }

    public SessionStatus getStatus() {
        return status;
    }

    void setStatus(SessionStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
