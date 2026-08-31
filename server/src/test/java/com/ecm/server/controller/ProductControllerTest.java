package com.ecm.server.controller;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.ProductFilterRequest;
import com.ecm.server.dto.response.ProductDetailResponse;
import com.ecm.server.dto.response.ProductSummaryResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.ProductService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getProducts_shouldReturnCursorPage() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductSummaryResponse summary = ProductSummaryResponse.builder()
                .id(productId)
                .name("iPhone 16 Pro Max")
                .seoName("iphone-16-pro-max")
                .minPrice(34990000L)
                .maxPrice(46990000L)
                .build();

        CursorPageResponse<ProductSummaryResponse> page = CursorPageResponse.<ProductSummaryResponse>builder()
                .items(List.of(summary))
                .hasNext(false)
                .size(1)
                .build();

        when(productService.getProducts(any(ProductFilterRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].name").value("iPhone 16 Pro Max"));
    }

    @Test
    void getProductById_shouldReturnProductDetail() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductDetailResponse detail = ProductDetailResponse.builder()
                .id(productId)
                .name("iPhone 16 Pro Max")
                .seoName("iphone-16-pro-max")
                .variants(List.of())
                .build();

        when(productService.getProductById(productId)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.seoName").value("iphone-16-pro-max"));
    }

    @Test
    void getProducts_whenPriceRangeIsReversed_shouldReturnStaticValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .queryParam("minPrice", "50000000")
                        .queryParam("maxPrice", "10000000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getProducts_whenLimitIsOutOfRange_shouldReturnStaticValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/products").queryParam("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("limit"));
    }
}
