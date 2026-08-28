package org.example.task;

public interface MockHttpClient {

    MockHttpResponse call(String url, String message);
}
