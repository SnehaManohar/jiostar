package org.example.task;

/**
 * No real network I/O is performed. Simulation rule: if {@code message}
 * contains the substring "fail" (case-insensitive), the call simulates a
 * server error (500); otherwise it simulates success (200).
 */
public class SimulatedMockHttpClient implements MockHttpClient {

    @Override
    public MockHttpResponse call(String url, String message) {
        boolean fail = message != null && message.toLowerCase().contains("fail");
        if (fail) {
            return new MockHttpResponse(500, "Simulated failure for url=" + url);
        }
        return new MockHttpResponse(200, "Simulated OK for url=" + url);
    }
}
