package com.ecm.server.controller;

import com.ecm.server.dto.request.UpdateOrderStatusRequest;
import com.ecm.server.dto.response.InvoiceResponse;
import com.ecm.server.dto.response.OrderDetailResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.AdminOrderService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminOrderService adminOrderService;

    @InjectMocks
    private AdminOrderController adminOrderController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminOrderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updateOrderStatus_shouldReturnUpdatedOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status("CONFIRM")
                .reason("Verified payment")
                .build();

        OrderDetailResponse response = OrderDetailResponse.builder()
                .id(orderId)
                .status("CONFIRM")
                .totalAmount(500000L)
                .build();

        when(adminOrderService.updateOrderStatus(eq(orderId), any(UpdateOrderStatusRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRM"));
    }

    @Test
    void getOrderInvoice_shouldReturnInvoiceResponse() throws Exception {
        UUID orderId = UUID.randomUUID();
        InvoiceResponse invoice = InvoiceResponse.builder()
                .invoiceId("INV-ABCD1234")
                .orderId(orderId)
                .customerName("Nguyen Van A")
                .totalAmount(500000L)
                .paymentStatus("PAID")
                .build();

        when(adminOrderService.getOrderInvoice(orderId)).thenReturn(invoice);

        mockMvc.perform(get("/api/v1/admin/orders/{id}/invoice", orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceId").value("INV-ABCD1234"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));
    }
}
