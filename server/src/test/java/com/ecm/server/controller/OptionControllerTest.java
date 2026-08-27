package com.ecm.server.controller;

import com.ecm.server.dto.request.CreateOptionRequest;
import com.ecm.server.dto.response.OptionResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.OptionService;
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
class OptionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OptionService optionService;

    @InjectMocks
    private OptionController optionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(optionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getOptions_shouldReturnList() throws Exception {
        UUID optionId = UUID.randomUUID();
        OptionResponse option = OptionResponse.builder()
                .id(optionId)
                .type("COLOR")
                .name("Titan Sa Mạc")
                .value("#C2B280")
                .status("ACTIVE")
                .build();

        when(optionService.getOptions("COLOR")).thenReturn(List.of(option));

        mockMvc.perform(get("/api/v1/options")
                        .param("type", "COLOR")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Titan Sa Mạc"));
    }

    @Test
    void createOption_whenValidPayload_shouldReturnCreated() throws Exception {
        CreateOptionRequest request = CreateOptionRequest.builder()
                .type("STORAGE")
                .name("256GB")
                .value("256GB")
                .build();

        OptionResponse response = OptionResponse.builder()
                .id(UUID.randomUUID())
                .type("STORAGE")
                .name("256GB")
                .value("256GB")
                .status("ACTIVE")
                .build();

        when(optionService.createOption(any(CreateOptionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(20100))
                .andExpect(jsonPath("$.data.name").value("256GB"));
    }
}
