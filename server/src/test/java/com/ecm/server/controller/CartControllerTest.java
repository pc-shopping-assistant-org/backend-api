package com.ecm.server.controller;

import com.ecm.server.dto.request.AddToCartRequest;
import com.ecm.server.dto.response.CartItemResponse;
import com.ecm.server.dto.response.CartResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.CartService;
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
class CartControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getCart_shouldReturnCartResponse() throws Exception {
        UUID variantId = UUID.randomUUID();
        CartItemResponse item = CartItemResponse.builder()
                .productVariantId(variantId)
                .productName("iPhone 16 Pro Max")
                .sku("IP16PM-DESERT-256")
                .listPrice(34990000L)
                .quantity(2)
                .subtotal(69980000L)
                .build();

        CartResponse cart = CartResponse.builder()
                .items(List.of(item))
                .totalItems(2)
                .subtotalAmount(69980000L)
                .build();

        when(cartService.getCart(any())).thenReturn(cart);

        mockMvc.perform(get("/api/v1/cart")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.items[0].sku").value("IP16PM-DESERT-256"));
    }

    @Test
    void addToCart_whenValidPayload_shouldReturnUpdatedCart() throws Exception {
        UUID variantId = UUID.randomUUID();
        AddToCartRequest request = AddToCartRequest.builder()
                .productVariantId(variantId)
                .quantity(1)
                .build();

        CartResponse cart = CartResponse.builder()
                .items(List.of())
                .totalItems(1)
                .subtotalAmount(34990000L)
                .build();

        when(cartService.addToCart(any(), any(AddToCartRequest.class))).thenReturn(cart);

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalItems").value(1));
    }
}
