package org.example.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulatedMockHttpClientTest {

    private final SimulatedMockHttpClient client = new SimulatedMockHttpClient();

    @Test
    void messageContainingFailReturns500() {
        assertEquals(500, client.call("https://example.com", "please fail").getStatusCode());
    }

    @Test
    void messageContainingFailIsCaseInsensitive() {
        assertEquals(500, client.call("https://example.com", "FAIL now").getStatusCode());
        assertEquals(500, client.call("https://example.com", "this will Failure").getStatusCode());
    }

    @Test
    void messageWithoutFailReturns200() {
        assertEquals(200, client.call("https://example.com", "ping").getStatusCode());
    }
}
