package com.ecm.server.controller;

import com.ecm.server.dto.request.CreateProductRequest;
import com.ecm.server.dto.response.ProductDetailResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.AdminProductService;
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
class AdminProductControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminProductService adminProductService;

    @InjectMocks
    private AdminProductController adminProductController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminProductController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createProduct_whenValidPayload_shouldReturnCreated() throws Exception {
        UUID categoryId = UUID.randomUUID();
        CreateProductRequest request = CreateProductRequest.builder()
                .name("MacBook Pro M3")
                .seoName("macbook-pro-m3")
                .categoryId(categoryId)
                .build();

        ProductDetailResponse response = ProductDetailResponse.builder()
                .id(UUID.randomUUID())
                .name("MacBook Pro M3")
                .seoName("macbook-pro-m3")
                .status("ACTIVE")
                .build();

        when(adminProductService.createProduct(any(CreateProductRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(20100))
                .andExpect(jsonPath("$.data.name").value("MacBook Pro M3"));
    }
}
