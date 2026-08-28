package org.example.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpTaskTest {

    @Test
    void status200MapsToSuccess() {
        MockHttpClient client = (url, message) -> new MockHttpResponse(200, "OK");
        TaskResult result = new HttpTask("https://example.com", "ping", client).execute();

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("200"));
    }

    @Test
    void status500MapsToFailure() {
        MockHttpClient client = (url, message) -> new MockHttpResponse(500, "boom");
        TaskResult result = new HttpTask("https://example.com", "please fail", client).execute();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("500"));
    }
}
