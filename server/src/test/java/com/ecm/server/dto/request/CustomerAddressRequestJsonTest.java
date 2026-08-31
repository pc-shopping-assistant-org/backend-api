package com.ecm.server.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerAddressRequestJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsCanonicalDefaultProperty() throws Exception {
        CustomerAddressRequest request = objectMapper.readValue("""
                {
                  "recipientName": "A",
                  "phone": "0912345678",
                  "addressLine": "Street",
                  "default": true
                }
                """, CustomerAddressRequest.class);

        assertTrue(request.isDefault());
        JsonNode output = objectMapper.valueToTree(request);
        assertTrue(output.get("default").asBoolean());
    }

    @Test
    void rejectsNonCanonicalIsDefaultProperty() {
        assertThrows(UnrecognizedPropertyException.class, () -> objectMapper.readValue("""
                {
                  "recipientName": "A",
                  "phone": "0912345678",
                  "addressLine": "Street",
                  "isDefault": true
                }
                """, CustomerAddressRequest.class));
    }
}
