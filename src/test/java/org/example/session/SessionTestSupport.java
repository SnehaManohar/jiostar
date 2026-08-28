package org.example.session;

import static org.junit.jupiter.api.Assertions.fail;

final class SessionTestSupport {

    private SessionTestSupport() {
    }

    static SessionStatusResponse awaitStatus(SessionManager manager, String sessionId, SessionStatus target, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        SessionStatusResponse response;
        do {
            response = manager.getSessionStatus(sessionId);
            if (response.getSessionStatus() == target) {
                return response;
            }
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting for session " + sessionId + " to reach " + target
                        + ", was " + response.getSessionStatus());
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Interrupted while awaiting status");
            }
        } while (true);
    }

    static void awaitSessionStatus(Session session, SessionStatus target, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (session.getStatus() != target) {
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting for session " + session.getId() + " to reach " + target
                        + ", was " + session.getStatus());
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Interrupted while awaiting status");
            }
        }
    }
}
