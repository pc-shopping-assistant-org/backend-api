package com.ecm.server.controller;

import com.ecm.server.dto.request.CreatePaymentIntentRequest;
import com.ecm.server.dto.response.PaymentIntentResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.PaymentService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createPaymentIntent_shouldReturnIntentResponse() throws Exception {
        UUID orderId = UUID.randomUUID();
        CreatePaymentIntentRequest request = CreatePaymentIntentRequest.builder()
                .orderId(orderId)
                .paymentMethod("STRIPE_CARD")
                .build();

        PaymentIntentResponse response = PaymentIntentResponse.builder()
                .paymentId(UUID.randomUUID())
                .orderId(orderId)
                .clientSecret("pi_123_secret_456")
                .amount(1000000L)
                .currency("VND")
                .publishableKey("pk_test_123")
                .build();

        when(paymentService.createPaymentIntent(any(), any(CreatePaymentIntentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/create-intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.clientSecret").value("pi_123_secret_456"))
                .andExpect(jsonPath("$.data.amount").value(1000000));
    }
}
