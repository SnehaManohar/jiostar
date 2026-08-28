package org.example.task;

public class TaskRequest {

    private final TaskType type;
    private final String message;
    private final String url;
    private final Long durationMillis;

    public TaskRequest(TaskType type, String message, String url, Long durationMillis) {
        this.type = type;
        this.message = message;
        this.url = url;
        this.durationMillis = durationMillis;
    }

    public TaskType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getUrl() {
        return url;
    }

    public Long getDurationMillis() {
        return durationMillis;
    }

    public void validate() {
        if (type == null) {
            throw new InvalidSessionRequestException("Task type is required");
        }
        switch (type) {
            case LOG -> {
                if (message == null || message.isEmpty()) {
                    throw new InvalidSessionRequestException("LOG task requires a message");
                }
            }
            case HTTP -> {
                if (url == null || url.isEmpty()) {
                    throw new InvalidSessionRequestException("HTTP task requires a url");
                }
                if (message == null || message.isEmpty()) {
                    throw new InvalidSessionRequestException("HTTP task requires a message");
                }
            }
            case SLEEP -> {
                if (durationMillis == null || durationMillis < 0) {
                    throw new InvalidSessionRequestException("SLEEP task requires a non-negative durationMillis");
                }
            }
        }
    }
}
