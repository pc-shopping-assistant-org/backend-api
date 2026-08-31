package com.ecm.server.controller;

import com.ecm.server.dto.request.CreateAttributeDefinitionRequest;
import com.ecm.server.dto.response.AttributeDefinitionResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.AttributeDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AttributeDefinitionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AttributeDefinitionService attributeDefinitionService;

    @InjectMocks
    private AttributeDefinitionController attributeDefinitionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(attributeDefinitionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllAttributes_shouldReturnList() throws Exception {
        UUID attrId = UUID.randomUUID();
        AttributeDefinitionResponse response = AttributeDefinitionResponse.builder()
                .id(attrId)
                .key("screen_size")
                .displayName("Kích thước màn hình")
                .dataType("NUMBER")
                .unit("inch")
                .build();

        when(attributeDefinitionService.getAllAttributes()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/attributes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].key").value("screen_size"));
    }

    @Test
    void createAttribute_whenValidPayload_shouldReturnCreated() throws Exception {
        CreateAttributeDefinitionRequest request = CreateAttributeDefinitionRequest.builder()
                .key("ram")
                .displayName("Dung lượng RAM")
                .dataType("STRING")
                .unit("GB")
                .build();

        AttributeDefinitionResponse response = AttributeDefinitionResponse.builder()
                .id(UUID.randomUUID())
                .key("ram")
                .displayName("Dung lượng RAM")
                .dataType("STRING")
                .unit("GB")
                .status("ACTIVE")
                .build();

        when(attributeDefinitionService.createAttribute(any(CreateAttributeDefinitionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/attributes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("CREATED"))
                .andExpect(jsonPath("$.data.key").value("ram"));
    }
}
