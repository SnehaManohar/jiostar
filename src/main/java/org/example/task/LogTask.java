package org.example.task;

public class LogTask implements Task {


    private final String message;

    public LogTask(String message) {
        this.message = message;
    }

    @Override
    public TaskResult execute() {
        System.out.println(message);
        return new TaskResult( "Logged task completed", true);
    }
}
