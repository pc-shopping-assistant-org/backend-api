package com.ecm.server.controller;

import com.ecm.server.dto.request.CreateOrderRequest;
import com.ecm.server.dto.request.OrderItemRequest;
import com.ecm.server.dto.response.OrderDetailResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.OrderService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createOrder_whenValidPayload_shouldReturnCreated() throws Exception {
        UUID variantId = UUID.randomUUID();
        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(OrderItemRequest.builder()
                        .productVariantId(variantId)
                        .quantity(1)
                        .build()))
                .recipientName("Nguyen Van A")
                .recipientPhone("0987654321")
                .deliveryAddress("123 Le Loi, TP.HCM")
                .paymentMethod("COD")
                .build();

        OrderDetailResponse response = OrderDetailResponse.builder()
                .id(UUID.randomUUID())
                .recipientName("Nguyen Van A")
                .recipientPhone("0987654321")
                .deliveryAddress("123 Le Loi, TP.HCM")
                .status("PENDING")
                .totalAmount(34990000L)
                .build();

        when(orderService.createOrder(any(), any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(20100))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(34990000));
    }
}
