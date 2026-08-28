# jiostar — Java Workflow/Task Engine

A single-user, in-process workflow engine. A client submits a batch of 1–10 tasks as a
**session**; sessions run concurrently with each other, tasks within a session run
strictly in submission order, and a session fails fast (without affecting any other
session) if one of its tasks fails.

## Requirements

- JDK 21+ (developed against JDK 25). No local Gradle install needed — use the bundled
  wrapper (`./gradlew`).

## How to run

```bash
./gradlew run
```

This starts an interactive terminal menu:

```
1) Submit session (terminal input)
2) Submit session (JSON input)
3) Check session status
4) Exit
```

- **Option 1** walks you through building 1–10 tasks via prompts (task type, then its
  parameters).
- **Option 2** accepts a pasted JSON array of tasks (see shape below), terminated by a
  blank line.
- **Option 3** prints a session's current status and the status/result of every task in
  it, given a session ID (printed when a session is submitted).
- **Option 4** exits. In-progress/queued sessions are allowed to run to completion first
  (no session can be cancelled once started) — see [Architecture](#architecture).

### JSON task shape

```json
[
  { "type": "LOG", "message": "starting job" },
  { "type": "HTTP", "url": "https://example.com/api", "message": "ping" },
  { "type": "SLEEP", "durationMillis": 500 }
]
```

`HTTP` calls are mocked — no real network I/O is performed. `HttpTask` uses
`SimulatedMockHttpClient`, which returns a simulated `500` if `message` contains the
substring `"fail"` (case-insensitive), else a simulated `200`. This is useful for
exercising the fail-fast path deliberately.

## Build & test

```bash
./gradlew build     # compile + run all tests
./gradlew test       # run tests only
./gradlew compileJava # compile main sources only
```

Test reports land in `build/reports/tests/test/index.html`; JUnit XML in
`build/test-results/test/`.

## Deployment

This is a CLI/library, not a server — "deployment" means producing a runnable artifact
and executing it wherever it's needed. The `application` Gradle plugin (already
configured in `build.gradle`, `mainClass = 'org.example.app.Main'`) builds this for you
with no extra plugins:

```bash
# Produce an installable distribution (start scripts + all dependency jars, incl. gson)
./gradlew installDist
./build/install/jiostar/bin/jiostar

# Or produce a shippable zip/tar to copy to another machine
./gradlew distZip     # -> build/distributions/jiostar-1.0-SNAPSHOT.zip
./gradlew distTar
```

Unzip/untar the distribution on the target machine (any machine with a JDK 21+ runtime)
and run `bin/jiostar` (or `bin/jiostar.bat` on Windows) — no separate JRE bundling or
containerization is required for this project's scope (single user, in-memory state).

## Build pipeline (CI)

A GitHub Actions workflow is included at
[`.github/workflows/build.yml`](.github/workflows/build.yml). It runs `./gradlew build`
(compile + full test suite) on every push and pull request targeting `main`, using the
Gradle wrapper so the CI Gradle version always matches what's committed. Once this
project is pushed to a GitHub remote, the workflow runs automatically — no further setup
needed. To extend it (e.g. add the `distZip` artifact as a build output, add a release
job), edit that file directly.

## Architecture

Four packages, each with a single responsibility:

- **`org.example.task`** — the 3 task types (`LogTask`, `HttpTask`, `SleepTask`), the
  `Task`/`DelayedTask`/`TaskResult` contracts, and `TaskFactory`/`TaskRequest` for
  turning client input into a runnable `Task`.
- **`org.example.session`** — the engine: `SessionManager` (submit/query API, owns the
  executors), `SessionRunner` (drives one session's tasks in order, non-blocking),
  `Session`/`TaskExecution` (mutable run state), `SessionStatusResponse` (read DTO).
- **`org.example.io`** — converts terminal prompts or a JSON array into
  `List<TaskRequest>`.
- **`org.example.app`** — `Main`, the CLI entry point / menu loop.

```mermaid
graph TB
    subgraph app["org.example.app"]
        Main["Main (CLI entry point)"]
    end

    subgraph io["org.example.io"]
        TerminalInputReader
        JsonTaskRequestParser
        TaskRequestJson
    end

    subgraph session["org.example.session"]
        SessionManager
        SessionRunner
        Session
        TaskExecution
        SessionStatusResponse
    end

    subgraph task["org.example.task"]
        TaskFactory
        TaskRequest
        Task
        DelayedTask
        LogTask
        HttpTask
        SleepTask
        MockHttpClient
    end

    Main --> TerminalInputReader
    Main --> JsonTaskRequestParser
    Main --> SessionManager
    JsonTaskRequestParser --> TaskRequestJson
    TerminalInputReader --> TaskRequest
    JsonTaskRequestParser --> TaskRequest
    SessionManager --> SessionRunner
    SessionManager --> Session
    SessionManager --> TaskFactory
    SessionManager --> SessionStatusResponse
    SessionRunner --> TaskExecution
    SessionRunner --> DelayedTask
    TaskFactory --> Task
    TaskFactory --> LogTask
    TaskFactory --> HttpTask
    TaskFactory --> SleepTask
    HttpTask --> MockHttpClient
```

### Why sleeping tasks don't block a worker thread

`SessionManager` owns two executors: a bounded `workExecutor` (fixed thread pool — where
task work actually runs) and a single-threaded `delayScheduler` (a pure timer — it only
fires a callback after a delay and holds no thread while waiting). A `SleepTask`
declares its wait via `getDelayMillis()` instead of calling `Thread.sleep()`.
`SessionRunner` realizes that wait on `delayScheduler` and only hands the task back to
`workExecutor` once the delay has elapsed — so a long sleep in one session never starves
other sessions of worker threads, even with a pool of size 1.

## Class diagram

```mermaid
classDiagram
    class Task {
        <<interface>>
        +execute() TaskResult
    }
    class DelayedTask {
        <<interface>>
        +getDelayMillis() long
    }
    class TaskResult {
        -Boolean success
        -String message
        -int runtime
        +isSuccess() boolean
        +getMessage() String
    }
    class LogTask {
        -String message
        +execute() TaskResult
    }
    class SleepTask {
        -long durationMillis
        +getDelayMillis() long
        +execute() TaskResult
    }
    class HttpTask {
        -String url
        -String message
        -MockHttpClient client
        +execute() TaskResult
    }
    class MockHttpClient {
        <<interface>>
        +call(url, message) MockHttpResponse
    }
    class SimulatedMockHttpClient {
        +call(url, message) MockHttpResponse
    }
    class MockHttpResponse {
        -int statusCode
        -String body
    }
    class TaskType {
        <<enumeration>>
        LOG
        HTTP
        SLEEP
    }
    class TaskRequest {
        -TaskType type
        -String message
        -String url
        -Long durationMillis
        +validate()
    }
    class TaskFactory {
        +createTask(TaskRequest) Task$
    }

    Task <|.. LogTask
    Task <|.. SleepTask
    Task <|.. HttpTask
    DelayedTask <|.. SleepTask
    MockHttpClient <|.. SimulatedMockHttpClient
    HttpTask --> MockHttpClient
    SimulatedMockHttpClient --> MockHttpResponse
    TaskRequest --> TaskType
    TaskFactory --> TaskRequest
    TaskFactory --> Task
    LogTask ..> TaskResult
    SleepTask ..> TaskResult
    HttpTask ..> TaskResult

    class TaskStatus {
        <<enumeration>>
        PENDING
        RUNNING
        SUCCESS
        FAILED
        SKIPPED
    }
    class SessionStatus {
        <<enumeration>>
        PENDING
        IN_PROGRESS
        COMPLETED
        FAILED
    }
    class TaskExecution {
        -TaskRequest request
        -Task task
        -TaskStatus status
        -TaskResult result
    }
    class Session {
        -String id
        -List~TaskExecution~ executions
        -SessionStatus status
        -Instant createdAt
    }
    class SessionRunner {
        -Session session
        -ExecutorService workExecutor
        -ScheduledExecutorService delayScheduler
        +start()
        -runNext(int index)
    }
    class SessionManager {
        -ExecutorService workExecutor
        -ScheduledExecutorService delayScheduler
        -Map~String,Session~ sessions
        +submitSession(List~TaskRequest~) String
        +getSessionStatus(String) SessionStatusResponse
        +shutdown()
    }
    class SessionStatusResponse {
        -String sessionId
        -SessionStatus sessionStatus
        -List~TaskStatusView~ taskStatuses
    }

    TaskExecution --> Task
    TaskExecution --> TaskRequest
    TaskExecution --> TaskStatus
    TaskExecution --> TaskResult
    Session --> TaskExecution
    Session --> SessionStatus
    SessionRunner --> Session
    SessionRunner --> DelayedTask
    SessionManager --> SessionRunner
    SessionManager --> Session
    SessionManager --> TaskFactory
    SessionManager --> SessionStatusResponse
    SessionStatusResponse --> TaskExecution
```

## Flow: submitting a session and checking its status

```mermaid
sequenceDiagram
    participant Client as Client (terminal/JSON)
    participant Main
    participant SM as SessionManager
    participant SR as SessionRunner
    participant WE as workExecutor
    participant DS as delayScheduler

    Client->>Main: choose input mode, provide tasks
    Main->>SM: submitSession(List~TaskRequest~)
    SM->>SM: validate size 1..10
    SM->>SM: build Session (id, TaskExecution list, PENDING)
    SM->>SR: new SessionRunner(session, workExecutor, delayScheduler).start()
    SM-->>Main: sessionId
    Main-->>Client: "Session submitted: <id>"

    SR->>WE: submit runNext(0)
    activate WE
    WE-->>SR: (async) begin task 0
    deactivate WE
    SR->>SR: session.status = IN_PROGRESS

    loop for each task in order
        alt task is a DelayedTask (SLEEP)
            SR->>DS: schedule(runTask, delayMillis)
            Note over DS: no thread held while waiting
            DS->>WE: submit runTask (after delay)
        else immediate task (LOG/HTTP)
            SR->>WE: submit runTask
        end
        WE->>WE: task.execute() -> TaskResult
        alt result.success
            WE->>SR: mark SUCCESS, runNext(index+1)
        else result.failure
            WE->>SR: mark FAILED, set failed=true, runNext(index+1)
        end
        opt failed == true
            SR->>SR: mark remaining tasks SKIPPED (never executed)
        end
    end
    SR->>SR: session.status = COMPLETED or FAILED

    Client->>Main: check status(sessionId)
    Main->>SM: getSessionStatus(sessionId)
    SM-->>Main: SessionStatusResponse (session + per-task status/result)
    Main-->>Client: printed status
```

## Task/session status lifecycle

```mermaid
stateDiagram-v2
    [*] --> PENDING: session submitted
    PENDING --> IN_PROGRESS: SessionRunner.start()
    IN_PROGRESS --> COMPLETED: all tasks SUCCESS
    IN_PROGRESS --> FAILED: any task fails
    COMPLETED --> [*]
    FAILED --> [*]

    state "Per-task status" as task_lifecycle {
        [*] --> TPENDING: created
        TPENDING --> TRUNNING: its turn in the sequence
        TRUNNING --> TSUCCESS: execute() returns success
        TRUNNING --> TFAILED: execute() returns failure
        TPENDING --> TSKIPPED: an earlier task in this session already failed
        TSUCCESS --> [*]
        TFAILED --> [*]
        TSKIPPED --> [*]
    }
```
