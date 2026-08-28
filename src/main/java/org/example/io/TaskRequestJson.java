package org.example.io;

/**
 * Mutable, no-arg-constructor POJO used purely as the Gson binding target
 * for JSON task input. Mapped into an immutable {@code TaskRequest} after parsing.
 */
class TaskRequestJson {
    String type;
    String message;
    String url;
    Long durationMillis;
}
