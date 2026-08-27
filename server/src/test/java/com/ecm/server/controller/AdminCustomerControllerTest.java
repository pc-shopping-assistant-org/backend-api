package com.ecm.server.controller;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.CustomerFilterRequest;
import com.ecm.server.dto.response.CustomerDetailResponse;
import com.ecm.server.dto.response.CustomerOrderSummaryResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.AdminCustomerService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminCustomerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminCustomerService adminCustomerService;

    @InjectMocks
    private AdminCustomerController adminCustomerController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminCustomerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getCustomers_shouldReturnCursorPageResponse() throws Exception {
        UUID customerId = UUID.randomUUID();
        CustomerDetailResponse detail = CustomerDetailResponse.builder()
                .id(customerId)
                .username("cust1")
                .fullName("Customer One")
                .email("cust1@example.com")
                .status("ACTIVE")
                .totalOrders(3)
                .totalSpent(450000)
                .build();

        CursorPageResponse<CustomerDetailResponse> pageResponse = CursorPageResponse.of(
                List.of(detail),
                10,
                item -> item.getId().toString()
        );

        when(adminCustomerService.getCustomers(any(CustomerFilterRequest.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/customers")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].username").value("cust1"));
    }

    @Test
    void getCustomerOrders_shouldReturnOrderList() throws Exception {
        UUID customerId = UUID.randomUUID();
        CustomerOrderSummaryResponse order = CustomerOrderSummaryResponse.builder()
                .orderId(UUID.randomUUID())
                .orderTime(Instant.now())
                .totalAmount(150000L)
                .discountAmount(0)
                .shipAmount(30000)
                .status("COMPLETED")
                .deliveryAddress("123 Test Street")
                .build();

        when(adminCustomerService.getCustomerOrders(customerId)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/admin/customers/" + customerId + "/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].totalAmount").value(150000));
    }
}
