package org.example.task;

public class TaskResult {

    public TaskResult(String message, Boolean success) {
//        this.runtime = runtime;
//        this.startTimestamp = startTimestamp;
        this.message = message;
        this.success = success;
    }

    private  Boolean success;
    private  String message;
    private  int runtime;

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public int getRuntime() {
        return runtime;
    }

    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    long startTimestamp;


}
