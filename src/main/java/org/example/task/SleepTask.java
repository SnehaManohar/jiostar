package org.example.task;

/**
 * The wait is expressed via {@link #getDelayMillis()}, not by blocking inside
 * {@link #execute()}. The session engine realizes the wait with a timer
 * (never parking a worker thread) and only calls {@link #execute()} once the
 * delay has already elapsed, so execute() itself completes immediately.
 */
public class SleepTask implements Task, DelayedTask {

    private final long durationMillis;

    public SleepTask(long durationMillis) {
        if (durationMillis < 0) {
            throw new IllegalArgumentException("durationMillis must be >= 0");
        }
        this.durationMillis = durationMillis;
    }

    @Override
    public long getDelayMillis() {
        return durationMillis;
    }

    @Override
    public TaskResult execute() {
        return new TaskResult("Slept for " + durationMillis + "ms", true);
    }
}
