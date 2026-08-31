package com.ecm.server.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiResponseJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesCanonicalEnvelopeWithStaticMessageKey() throws Exception {
        ApiResponse<Void> response = ApiResponse.error(
                StatusCode.VALIDATION_ERROR,
                "Email is already in use",
                Map.of("email", "already exists"));

        var json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        List<String> fields = new ArrayList<>();
        json.fieldNames().forEachRemaining(fields::add);
        assertEquals(List.of("data", "message", "errors"), fields);
        assertEquals("null", json.get("data").toString());
        assertEquals(StatusCode.VALIDATION_ERROR.name(), json.get("message").asText());
        assertNotNull(json.get("errors"));
        assertEquals("Email is already in use", json.get("errors").get(0).get("message").asText());
        assertEquals("already exists", json.get("errors").get(1).get("message").asText());
        assertFalse(json.has("success"));
        assertFalse(json.has("code"));
    }
}
