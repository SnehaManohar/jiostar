package org.example.task;

public class InvalidSessionRequestException extends RuntimeException {

    public InvalidSessionRequestException(String message) {
        super(message);
    }
}
