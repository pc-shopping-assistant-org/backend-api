package com.ecm.server.service;

import com.ecm.server.dto.request.AddToCartRequest;
import com.ecm.server.dto.request.UpdateCartItemRequest;
import com.ecm.server.dto.response.CartResponse;

import java.util.UUID;

public interface CartService {

    CartResponse getCart(UUID accountId);

    CartResponse getCart(UUID accountId, String sessionToken);

    CartResponse addToCart(UUID accountId, AddToCartRequest request);

    CartResponse addToCart(UUID accountId, String sessionToken, AddToCartRequest request);

    CartResponse updateCartItem(UUID accountId, UUID variantId, UpdateCartItemRequest request);

    CartResponse updateCartItem(UUID accountId, String sessionToken, UUID variantId, UpdateCartItemRequest request);

    CartResponse removeCartItem(UUID accountId, UUID variantId);

    CartResponse removeCartItem(UUID accountId, String sessionToken, UUID variantId);

    void clearCart(UUID accountId);

    void clearCart(UUID accountId, String sessionToken);
}
