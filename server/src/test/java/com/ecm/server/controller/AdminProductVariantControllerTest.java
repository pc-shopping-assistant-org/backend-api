package com.ecm.server.controller;

import com.ecm.server.dto.request.CreateProductVariantRequest;
import com.ecm.server.dto.response.ProductVariantResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.AdminProductVariantService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminProductVariantControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminProductVariantService adminProductVariantService;

    @InjectMocks
    private AdminProductVariantController adminProductVariantController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminProductVariantController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createVariant_whenValidPayload_shouldReturnCreated() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateProductVariantRequest request = CreateProductVariantRequest.builder()
                .sku("IPHONE16-DESERT-256")
                .price(34990000)
                .priceSale(33990000)
                .quantity(50)
                .build();

        ProductVariantResponse response = ProductVariantResponse.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .sku("IPHONE16-DESERT-256")
                .price(34990000)
                .priceSale(33990000)
                .quantity(50)
                .status("ACTIVE")
                .build();

        when(adminProductVariantService.createVariant(eq(productId), any(CreateProductVariantRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/products/{productId}/variants", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(20100))
                .andExpect(jsonPath("$.data.sku").value("IPHONE16-DESERT-256"));
    }
}
