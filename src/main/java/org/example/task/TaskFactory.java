package org.example.task;

public final class TaskFactory {

    private TaskFactory() {
    }

    public static Task createTask(TaskRequest request) {
        request.validate();
        return switch (request.getType()) {
            case LOG -> new LogTask(request.getMessage());
            case SLEEP -> new SleepTask(request.getDurationMillis());
            case HTTP -> new HttpTask(request.getUrl(), request.getMessage(), new SimulatedMockHttpClient());
        };
    }
}
