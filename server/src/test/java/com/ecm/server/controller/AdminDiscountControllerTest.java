package com.ecm.server.controller;

import com.ecm.server.dto.request.CreateDiscountRequest;
import com.ecm.server.dto.response.DiscountDetailResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.AdminDiscountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminDiscountControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private AdminDiscountService adminDiscountService;

    @InjectMocks
    private AdminDiscountController adminDiscountController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminDiscountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createDiscount_whenValidPayload_shouldReturnCreated() throws Exception {
        CreateDiscountRequest request = CreateDiscountRequest.builder()
                .code("FLASH50")
                .title("Flash Sale 50k")
                .type("FIXED")
                .value(50000)
                .scope("ALL")
                .minOrderAmount(200000L)
                .startAt(Instant.now())
                .endAt(Instant.now().plusSeconds(86400))
                .build();

        DiscountDetailResponse response = DiscountDetailResponse.builder()
                .id(UUID.randomUUID())
                .code("FLASH50")
                .title("Flash Sale 50k")
                .type("FIXED")
                .value(50000)
                .scope("ALL")
                .minOrderAmount(200000L)
                .status("ACTIVE")
                .appliedVariants(List.of())
                .build();

        when(adminDiscountService.createDiscount(any(CreateDiscountRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/discounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(20100))
                .andExpect(jsonPath("$.data.code").value("FLASH50"));
    }
}
