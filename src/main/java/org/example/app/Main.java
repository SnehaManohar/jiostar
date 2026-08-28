package org.example.app;

import org.example.io.JsonTaskRequestParser;
import org.example.io.TerminalInputReader;
import org.example.session.SessionManager;
import org.example.session.SessionStatusResponse;
import org.example.task.TaskRequest;

import java.util.List;
import java.util.Scanner;

public class Main {

    private static final int POOL_SIZE = 1;

    public static void main(String[] args) {
        SessionManager sessionManager = new SessionManager(POOL_SIZE);
        Scanner scanner = new Scanner(System.in);
        TerminalInputReader terminalReader = new TerminalInputReader();
        JsonTaskRequestParser jsonParser = new JsonTaskRequestParser();

        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> {
                        List<TaskRequest> requests = terminalReader.readTaskRequests(scanner);
                        submitAndPrint(sessionManager, requests);
                    }
                    case "2" -> {
                        String json = readMultilineJson(scanner);
                        List<TaskRequest> requests = jsonParser.parse(json);
                        submitAndPrint(sessionManager, requests);
                    }
                    case "3" -> {
                        System.out.print("Enter session ID: ");
                        String id = scanner.nextLine().trim();
                        printStatus(sessionManager.getSessionStatus(id));
                    }
                    case "4" -> running = false;
                    default -> System.out.println("Invalid choice");
                }
            } catch (RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        System.out.println("Shutting down. Waiting for in-progress sessions to finish...");
        sessionManager.shutdown();
        System.out.println("Goodbye.");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("1) Submit session (terminal input)");
        System.out.println("2) Submit session (JSON input)");
        System.out.println("3) Check session status");
        System.out.println("4) Exit");
        System.out.print("Choose an option: ");
    }

    private static void submitAndPrint(SessionManager sessionManager, List<TaskRequest> requests) {
        String sessionId = sessionManager.submitSession(requests);
        System.out.println("Session submitted: " + sessionId);
    }

    private static String readMultilineJson(Scanner scanner) {
        System.out.println("Paste JSON task array. End with a blank line:");
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.isBlank()) {
                break;
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static void printStatus(SessionStatusResponse response) {
        System.out.println("Session " + response.getSessionId() + " status: " + response.getSessionStatus());
        for (SessionStatusResponse.TaskStatusView view : response.getTaskStatuses()) {
            String resultMessage = view.getResult() == null ? "(no result yet)" : view.getResult().getMessage();
            System.out.println("  [" + view.getIndex() + "] " + view.getType() + " -> " + view.getStatus()
                    + " : " + resultMessage);
        }
    }
}
