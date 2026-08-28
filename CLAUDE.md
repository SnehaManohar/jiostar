# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build              # compile + run all tests
./gradlew test                # run tests only
./gradlew compileJava          # compile main sources only
./gradlew run                  # run the interactive CLI (org.example.app.Main)

# Run a single test class or method
./gradlew test --tests "org.example.session.SessionRunnerTest"
./gradlew test --tests "org.example.session.SessionRunnerTest.skippedTasksNeverHaveExecuteCalled"

# Produce a runnable distribution (for handing off/deploying elsewhere)
./gradlew installDist && ./build/install/jiostar/bin/jiostar
```

JUnit 5 (via `junit-jupiter`), test reports at `build/reports/tests/test/index.html`.

## Architecture

This is a single-user, in-process workflow engine: a client submits a batch of 1–10
tasks as a **session**; sessions run concurrently with each other; tasks *within* one
session run strictly in submission order; a session fails fast (without affecting any
other session) the moment one of its tasks fails. There is no cancel API — once started,
a session always runs to completion. Full diagrams (architecture, class, sequence,
status-lifecycle) are in `README.md` — read those before making structural changes.

Four packages, each a distinct layer:

- **`org.example.task`** — the 3 task types and how a client's input becomes a runnable
  `Task`. `TaskFactory.createTask(TaskRequest)` is the only place task construction
  happens; adding a 4th task type means adding a `TaskType` enum value, a new `Task`
  implementation, a case in `TaskFactory`, and validation rules in
  `TaskRequest.validate()`.
- **`org.example.session`** — the actual engine. `SessionManager` is the public API
  (`submitSession`, `getSessionStatus`, `shutdown`) and owns two executors. `SessionRunner`
  drives one session.
- **`org.example.io`** — two independent front-ends (`TerminalInputReader`,
  `JsonTaskRequestParser`) that both converge on the same `List<TaskRequest>`, so
  `SessionManager` never needs to know which input mode was used.
- **`org.example.app`** — `Main`, a menu loop over the two input modes plus status
  lookup.

### The non-blocking-sleep design (the trickiest part of this codebase)

`SessionManager` owns **two** executors, not one:
- `workExecutor` — a bounded fixed thread pool where task work (`Task.execute()`)
  actually runs.
- `delayScheduler` — a single-threaded `ScheduledExecutorService` used *only* as a timer.
  It never runs task work; it just fires a callback after a delay and hands off to
  `workExecutor`. The JDK's delay-queue mechanism holds no thread while waiting, which is
  the whole point.

A task that needs to wait (currently only `SleepTask`) implements `DelayedTask`
(`getDelayMillis()`) instead of blocking inside `execute()`. `SleepTask.execute()`
itself returns immediately — the actual wait is realized entirely by
`SessionRunner`/`delayScheduler`, never by `Thread.sleep()` on a pool thread. This means
a session sleeping for seconds never starves other sessions of worker threads, even with
`workExecutor` sized to a single thread.

`SessionRunner` is **not** a blocking loop — it's a recursive callback chain
(`start()` → `runNext(index)`). Each task's continuation is only submitted (to
`delayScheduler` if it's a `DelayedTask`, otherwise straight to `workExecutor`) after the
previous task has fully finished (or been marked `SKIPPED`), which is what keeps a
session's tasks strictly sequential despite the whole thing being asynchronous. The
`failed` flag inside `SessionRunner` is a plain (non-volatile) field — it's safe without
synchronization because every hand-off between steps goes through an `ExecutorService`
submission, which the JMM guarantees establishes happens-before ordering. If you ever
change `SessionRunner` to run steps outside that submit/schedule chain (e.g. a direct
method call bypassing the executor), that safety argument breaks.

`TaskExecution.status`/`.result` and `Session.status` **are** `volatile` — those get
read by `SessionManager.getSessionStatus()` from an arbitrary caller thread while a
session is still running, so they need real visibility guarantees independent of the
executor hand-off chain.

### Fail-fast semantics

Fail-fast is per-session only. `SessionRunner` tracks a local `failed` flag; once a task
fails, every subsequent task in that same session's list is marked `SKIPPED` and never
has `execute()` called (this exact invariant — that a skipped task truly never runs — is
what `SessionRunnerTest` asserts using spy tasks with `AtomicBoolean` flags, since
`SessionStatus.SKIPPED` alone doesn't prove non-execution). Other sessions are
structurally isolated (separate `Session`/`SessionRunner` instances, no shared mutable
state), so one session's failure can never affect another's.

### Mocked HTTP

`HttpTask` never performs real network I/O — it delegates to a `MockHttpClient`.
`SimulatedMockHttpClient` (the default) returns a simulated 500 if the task's `message`
contains `"fail"` (case-insensitive), else 200. This is the standard way tests and manual
runs exercise the fail-fast path deliberately — submit an `HTTP` task whose message
contains `"fail"`.
