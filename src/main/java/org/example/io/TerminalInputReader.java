package org.example.io;

import org.example.task.TaskRequest;
import org.example.task.TaskType;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TerminalInputReader {

    public List<TaskRequest> readTaskRequests(Scanner scanner) {
        int count = readTaskCount(scanner);
        List<TaskRequest> requests = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            requests.add(readOneTaskRequest(scanner, i));
        }
        return requests;
    }

    private int readTaskCount(Scanner scanner) {
        while (true) {
            System.out.print("How many tasks in this session? (1-10): ");
            String line = scanner.nextLine().trim();
            try {
                int count = Integer.parseInt(line);
                if (count >= 1 && count <= 10) {
                    return count;
                }
            } catch (NumberFormatException ignored) {
                // fall through to re-prompt
            }
            System.out.println("Please enter a whole number between 1 and 10.");
        }
    }

    private TaskRequest readOneTaskRequest(Scanner scanner, int index) {
        TaskType type = readTaskType(scanner, index);
        return switch (type) {
            case LOG -> {
                String message = readNonEmptyString(scanner, "Enter log message: ");
                yield new TaskRequest(TaskType.LOG, message, null, null);
            }
            case HTTP -> {
                String url = readNonEmptyString(scanner, "Enter URL: ");
                String message = readNonEmptyString(scanner, "Enter message: ");
                yield new TaskRequest(TaskType.HTTP, message, url, null);
            }
            case SLEEP -> {
                long durationMillis = readNonNegativeLong(scanner, "Enter sleep duration in milliseconds: ");
                yield new TaskRequest(TaskType.SLEEP, null, null, durationMillis);
            }
        };
    }

    private TaskType readTaskType(Scanner scanner, int index) {
        while (true) {
            System.out.print("Task " + index + " type - choose one [1=LOG, 2=HTTP, 3=SLEEP]: ");
            String line = scanner.nextLine().trim();
            switch (line) {
                case "1":
                    return TaskType.LOG;
                case "2":
                    return TaskType.HTTP;
                case "3":
                    return TaskType.SLEEP;
                default:
                    System.out.println("Please enter 1, 2, or 3.");
            }
        }
    }

    private String readNonEmptyString(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("Value cannot be empty.");
        }
    }

    private long readNonNegativeLong(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                long value = Long.parseLong(line);
                if (value >= 0) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to re-prompt
            }
            System.out.println("Please enter a non-negative whole number.");
        }
    }
}
