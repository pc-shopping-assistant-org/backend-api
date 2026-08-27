package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.config.security.UserPrincipal;
import com.ecm.server.dto.request.AddToCartRequest;
import com.ecm.server.dto.request.UpdateCartItemRequest;
import com.ecm.server.dto.response.CartResponse;
import com.ecm.server.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CartResponse response = cartService.getCart(principal.getAccountId());
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CartResponse response = cartService.addToCart(principal.getAccountId(), request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PutMapping("/items/{variantId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CartResponse response = cartService.updateCartItem(principal.getAccountId(), variantId, request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @PathVariable UUID variantId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CartResponse response = cartService.removeCartItem(principal.getAccountId(), variantId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> clearCart(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        cartService.clearCart(principal.getAccountId());
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Cart cleared successfully"));
    }
}
