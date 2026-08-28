package org.example.task;

public class HttpTask implements Task {

    private final String url;
    private final String message;
    private final MockHttpClient client;

    public HttpTask(String url, String message, MockHttpClient client) {
        this.url = url;
        this.message = message;
        this.client = client;
    }

    @Override
    public TaskResult execute() {
        MockHttpResponse response = client.call(url, message);
        boolean success = response.getStatusCode() >= 200 && response.getStatusCode() < 300;
        String resultMessage = "HTTP " + response.getStatusCode() + ": " + response.getBody();
        return new TaskResult(resultMessage, success);
    }
}
