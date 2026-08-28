package org.example.task;

/**
 * Optional contract for a {@link Task} whose work involves waiting before it
 * actually runs (e.g. a sleep). Callers driving execution (the session
 * engine) use {@link #getDelayMillis()} to realize the wait via a timer
 * rather than by blocking a worker thread inside {@link Task#execute()}.
 */
public interface DelayedTask {

    long getDelayMillis();
}
