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
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Cart-Session", required = false) String sessionToken
    ) {
        CartResponse response = principal != null
                ? cartService.getCart(principal.getAccountId())
                : sessionToken == null ? cartService.getCart(null) : cartService.getCart(null, sessionToken);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Cart-Session", required = false) String sessionToken
    ) {
        CartResponse response = principal != null
                ? cartService.addToCart(principal.getAccountId(), request)
                : sessionToken == null ? cartService.addToCart(null, request) : cartService.addToCart(null, sessionToken, request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PutMapping("/items/{variantId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Cart-Session", required = false) String sessionToken
    ) {
        CartResponse response = principal != null
                ? cartService.updateCartItem(principal.getAccountId(), variantId, request)
                : sessionToken == null ? cartService.updateCartItem(null, variantId, request)
                : cartService.updateCartItem(null, sessionToken, variantId, request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @PathVariable UUID variantId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Cart-Session", required = false) String sessionToken
    ) {
        CartResponse response = principal != null
                ? cartService.removeCartItem(principal.getAccountId(), variantId)
                : sessionToken == null ? cartService.removeCartItem(null, variantId)
                : cartService.removeCartItem(null, sessionToken, variantId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> clearCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Cart-Session", required = false) String sessionToken
    ) {
        if (principal != null) {
            cartService.clearCart(principal.getAccountId());
        } else if (sessionToken == null) {
            cartService.clearCart(null);
        } else {
            cartService.clearCart(null, sessionToken);
        }
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Cart cleared successfully"));
    }
}
