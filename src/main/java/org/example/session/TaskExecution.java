package org.example.session;

import org.example.task.Task;
import org.example.task.TaskRequest;
import org.example.task.TaskResult;

public class TaskExecution {

    private final TaskRequest request;
    private final Task task;
    private volatile TaskStatus status = TaskStatus.PENDING;
    private volatile TaskResult result;

    public TaskExecution(TaskRequest request, Task task) {
        this.request = request;
        this.task = task;
    }

    public TaskRequest getRequest() {
        return request;
    }

    public Task getTask() {
        return task;
    }

    public TaskStatus getStatus() {
        return status;
    }

    void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskResult getResult() {
        return result;
    }

    void setResult(TaskResult result) {
        this.result = result;
    }
}
