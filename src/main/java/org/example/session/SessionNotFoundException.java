package org.example.session;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(String sessionId) {
        super("No session found with id " + sessionId);
    }
}
