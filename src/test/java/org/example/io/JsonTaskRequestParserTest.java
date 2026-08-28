package org.example.io;

import org.example.task.InvalidSessionRequestException;
import org.example.task.TaskRequest;
import org.example.task.TaskType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonTaskRequestParserTest {

    private final JsonTaskRequestParser parser = new JsonTaskRequestParser();

    @Test
    void parsesAllThreeTypesInOrder() {
        String json = """
                [
                  { "type": "LOG", "message": "starting job" },
                  { "type": "HTTP", "url": "https://example.com/api", "message": "ping" },
                  { "type": "SLEEP", "durationMillis": 500 }
                ]
                """;

        List<TaskRequest> requests = parser.parse(json);

        assertEquals(3, requests.size());
        assertEquals(TaskType.LOG, requests.get(0).getType());
        assertEquals("starting job", requests.get(0).getMessage());
        assertEquals(TaskType.HTTP, requests.get(1).getType());
        assertEquals("https://example.com/api", requests.get(1).getUrl());
        assertEquals("ping", requests.get(1).getMessage());
        assertEquals(TaskType.SLEEP, requests.get(2).getType());
        assertEquals(500L, requests.get(2).getDurationMillis());
    }

    @Test
    void malformedJsonThrows() {
        assertThrows(InvalidSessionRequestException.class, () -> parser.parse("not json at all"));
    }

    @Test
    void unknownTypeThrows() {
        String json = "[{ \"type\": \"CARRIER_PIGEON\", \"message\": \"hi\" }]";
        assertThrows(InvalidSessionRequestException.class, () -> parser.parse(json));
    }

    @Test
    void missingRequiredFieldThrows() {
        String json = "[{ \"type\": \"HTTP\", \"message\": \"hi\" }]";
        assertThrows(InvalidSessionRequestException.class, () -> parser.parse(json));
    }
}
