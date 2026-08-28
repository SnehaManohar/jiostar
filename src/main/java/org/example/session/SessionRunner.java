package org.example.session;

import org.example.task.DelayedTask;
import org.example.task.TaskResult;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Drives one session's tasks strictly in order without ever blocking a
 * {@code workExecutor} thread on a task's wait. A {@link DelayedTask} (e.g.
 * SLEEP) has its wait realized via {@code delayScheduler}'s timer queue,
 * which holds no thread while waiting; the next task is only submitted once
 * the previous one has fully completed, which keeps the session sequential
 * while freeing worker threads to service other sessions in the meantime.
 */
public class SessionRunner {

    private final Session session;
    private final ExecutorService workExecutor;
    private final ScheduledExecutorService delayScheduler;
    private boolean failed = false;

    public SessionRunner(Session session, ExecutorService workExecutor, ScheduledExecutorService delayScheduler) {
        this.session = session;
        this.workExecutor = workExecutor;
        this.delayScheduler = delayScheduler;
    }

    public void start() {
        session.setStatus(SessionStatus.IN_PROGRESS);
        workExecutor.execute(() -> runNext(0));
    }

    private void runNext(int index) {
        List<TaskExecution> executions = session.getExecutions();
        if (index >= executions.size()) {
            session.setStatus(failed ? SessionStatus.FAILED : SessionStatus.COMPLETED);
            return;
        }

        TaskExecution exec = executions.get(index);
        if (failed) {
            exec.setStatus(TaskStatus.SKIPPED);
            workExecutor.execute(() -> runNext(index + 1));
            return;
        }

        exec.setStatus(TaskStatus.RUNNING);
        long delayMillis = exec.getTask() instanceof DelayedTask delayedTask ? delayedTask.getDelayMillis() : 0;

        Runnable runTask = () -> {
            TaskResult result = exec.getTask().execute();
            exec.setResult(result);
            if (result != null && result.isSuccess()) {
                exec.setStatus(TaskStatus.SUCCESS);
            } else {
                exec.setStatus(TaskStatus.FAILED);
                failed = true;
            }
            runNext(index + 1);
        };

        if (delayMillis > 0) {
            delayScheduler.schedule(() -> workExecutor.execute(runTask), delayMillis, TimeUnit.MILLISECONDS);
        } else {
            workExecutor.execute(runTask);
        }
    }
}
