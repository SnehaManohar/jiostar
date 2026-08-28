package org.example.io;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.example.task.InvalidSessionRequestException;
import org.example.task.TaskRequest;
import org.example.task.TaskType;

import java.util.ArrayList;
import java.util.List;

public class JsonTaskRequestParser {

    private static final Gson GSON = new Gson();

    public List<TaskRequest> parse(String json) {
        TaskRequestJson[] raw;
        try {
            raw = GSON.fromJson(json, TaskRequestJson[].class);
        } catch (JsonSyntaxException e) {
            throw new InvalidSessionRequestException("Malformed JSON: " + e.getMessage());
        }
        if (raw == null) {
            throw new InvalidSessionRequestException("JSON task array must not be empty");
        }
        List<TaskRequest> requests = new ArrayList<>();
        for (TaskRequestJson item : raw) {
            requests.add(toTaskRequest(item));
        }
        return requests;
    }

    private TaskRequest toTaskRequest(TaskRequestJson item) {
        if (item.type == null) {
            throw new InvalidSessionRequestException("Task JSON is missing required field 'type'");
        }
        TaskType type;
        try {
            type = TaskType.valueOf(item.type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidSessionRequestException("Unknown task type: " + item.type);
        }
        TaskRequest request = new TaskRequest(type, item.message, item.url, item.durationMillis);
        request.validate();
        return request;
    }
}
