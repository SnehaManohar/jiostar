package org.example.task;

public class MockHttpResponse {

    private final int statusCode;
    private final String body;

    public MockHttpResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }
}
