package org.example.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SleepTaskTest {

    @Test
    void exposesConfiguredDurationAsDelay() {
        SleepTask task = new SleepTask(150);
        assertEquals(150L, task.getDelayMillis());
    }

    @Test
    void executeReturnsImmediatelyWithoutBlocking() {
        SleepTask task = new SleepTask(500);

        long start = System.currentTimeMillis();
        TaskResult result = task.execute();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 100, "execute() must not block on the delay itself, took " + elapsed + "ms");
        assertTrue(result.isSuccess());
    }

    @Test
    void rejectsNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () -> new SleepTask(-1));
    }

    @Test
    void zeroDurationSucceeds() {
        TaskResult result = new SleepTask(0).execute();
        assertEquals(true, result.isSuccess());
    }
}
