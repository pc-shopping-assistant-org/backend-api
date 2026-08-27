package com.ecm.server.controller;

import com.ecm.server.dto.request.UpdatePaymentStatusRequest;
import com.ecm.server.dto.response.PaymentDetailResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.AdminPaymentService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminPaymentControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminPaymentService adminPaymentService;

    @InjectMocks
    private AdminPaymentController adminPaymentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminPaymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updatePaymentStatus_shouldReturnUpdatedPayment() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UpdatePaymentStatusRequest request = UpdatePaymentStatusRequest.builder()
                .status("PAID")
                .transactionCode("BANK_TRANS_999")
                .build();

        PaymentDetailResponse response = PaymentDetailResponse.builder()
                .id(paymentId)
                .status("PAID")
                .transactionCode("BANK_TRANS_999")
                .build();

        when(adminPaymentService.updatePaymentStatus(eq(paymentId), any(UpdatePaymentStatusRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/payments/{id}/status", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.transactionCode").value("BANK_TRANS_999"));
    }
}
