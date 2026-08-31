package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.AddToCartRequest;
import com.ecm.server.dto.request.UpdateCartItemRequest;
import com.ecm.server.dto.response.CartItemResponse;
import com.ecm.server.dto.response.CartResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.model.Cart;
import com.ecm.server.model.CartItem;
import com.ecm.server.model.CartItemId;
import com.ecm.server.model.Customer;
import com.ecm.server.model.File;
import com.ecm.server.model.ProductImage;
import com.ecm.server.model.ProductVariant;
import com.ecm.server.repository.CartRepository;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.repository.ProductVariantRepository;
import com.ecm.server.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final CartRepository cartRepository;
    private final CustomerRepository customerRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID accountId) {
        return getCart(accountId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID accountId, String sessionToken) {
        Cart cart = findActiveCart(accountId, sessionToken, false).orElse(null);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(UUID accountId, AddToCartRequest request) {
        return addToCart(accountId, null, request);
    }

    @Override
    @Transactional
    public CartResponse addToCart(UUID accountId, String sessionToken, AddToCartRequest request) {
        requireOwner(accountId, sessionToken);
        // Keep the lock order identical to checkout (cart -> variant).  The
        // cart lock serializes mutations for one owner, while the variant row
        // lock makes the stock check observe the current quantity instead of
        // a value read before a concurrent checkout decremented it.
        Cart cart = getOrCreateActiveCart(accountId, sessionToken);
        ProductVariant variant = findSellableVariantForUpdate(request.getProductVariantId());
        CartItem item = cart.getItems().stream()
                .filter(candidate -> candidate.getVariant().getId().equals(variant.getId()))
                .findFirst()
                .orElseGet(() -> {
                    CartItem newItem = CartItem.builder()
                            .id(new CartItemId(cart.getId(), variant.getId()))
                            .cart(cart)
                            .variant(variant)
                            .quantity(0)
                            .build();
                    cart.getItems().add(newItem);
                    return newItem;
                });

        int newQuantity;
        try {
            newQuantity = Math.addExact(item.getQuantity(), request.getQuantity());
        } catch (ArithmeticException ex) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Cart item quantity is too large");
        }
        ensureStock(variant, newQuantity);
        item.setQuantity(newQuantity);
        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(UUID accountId, UUID variantId, UpdateCartItemRequest request) {
        return updateCartItem(accountId, null, variantId, request);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(UUID accountId, String sessionToken, UUID variantId, UpdateCartItemRequest request) {
        requireOwner(accountId, sessionToken);
        Cart cart = getActiveCartOrThrow(accountId, sessionToken);
        ProductVariant variant = findSellableVariantForUpdate(variantId);
        CartItem item = findItem(cart, variantId);
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException(StatusCode.VALIDATION_ERROR, "Cart item quantity must be greater than zero");
        }
        ensureStock(variant, request.getQuantity());
        item.setQuantity(request.getQuantity());
        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(UUID accountId, UUID variantId) {
        return removeCartItem(accountId, null, variantId);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(UUID accountId, String sessionToken, UUID variantId) {
        requireOwner(accountId, sessionToken);
        Cart cart = getActiveCartOrThrow(accountId, sessionToken);
        CartItem item = findItem(cart, variantId);
        cart.removeItem(item);
        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(UUID accountId) {
        clearCart(accountId, null);
    }

    @Override
    @Transactional
    public void clearCart(UUID accountId, String sessionToken) {
        requireOwner(accountId, sessionToken);
        findActiveCartForUpdate(accountId, sessionToken).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    private Cart getOrCreateActiveCart(UUID accountId, String sessionToken) {
        return findActiveCartForUpdate(accountId, sessionToken).orElseGet(() -> {
            Cart cart = Cart.builder()
                    .customer(resolveCustomer(accountId))
                    .sessionToken(normalizeSession(sessionToken))
                    .status(STATUS_ACTIVE)
                    .build();
            try {
                return cartRepository.saveAndFlush(cart);
            } catch (DataIntegrityViolationException ex) {
                // The failed INSERT aborts the current PostgreSQL transaction,
                // so querying for the winner here is not a valid recovery path.
                // Let the request retry in a fresh transaction instead of
                // returning an unusable EntityManager or creating a second
                // active cart outside the unique-index invariant.
                log.info("Active cart creation conflicted for owner; rejecting request for retry");
                throw new BusinessException(StatusCode.CONFLICT,
                        "An active cart already exists for this owner");
            }
        });
    }

    private java.util.Optional<Cart> findActiveCart(UUID accountId, String sessionToken, boolean requiredOwner) {
        requireOwner(accountId, sessionToken, requiredOwner);
        if (accountId != null) {
            return cartRepository.findActiveByAccountId(accountId);
        }
        if (sessionToken == null || sessionToken.isBlank()) {
            return java.util.Optional.empty();
        }
        return cartRepository.findActiveBySessionToken(sessionToken.trim());
    }

    private Cart getActiveCartOrThrow(UUID accountId, String sessionToken) {
        return findActiveCartForUpdate(accountId, sessionToken)
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Active cart not found"));
    }

    private java.util.Optional<Cart> findActiveCartForUpdate(UUID accountId, String sessionToken) {
        requireOwner(accountId, sessionToken, true);
        if (accountId != null) {
            return cartRepository.findActiveByAccountIdForUpdate(accountId);
        }
        return cartRepository.findActiveBySessionTokenForUpdate(sessionToken.trim());
    }

    private Customer resolveCustomer(UUID accountId) {
        if (accountId == null) {
            return null;
        }
        return customerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.CUSTOMER_NOT_FOUND));
    }

    private ProductVariant findSellableVariantForUpdate(UUID variantId) {
        return productVariantRepository.findActiveByIdForUpdate(variantId)
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));
    }

    private CartItem findItem(Cart cart, UUID variantId) {
        return cart.getItems().stream()
                .filter(item -> item.getVariant().getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Cart item not found"));
    }

    private void ensureStock(ProductVariant variant, int requestedQuantity) {
        if (requestedQuantity > variant.getQuantity()) {
            throw new BusinessException(StatusCode.INSUFFICIENT_STOCK,
                    "Requested quantity exceeds available stock (" + variant.getQuantity() + ")");
        }
    }

    private CartResponse toResponse(Cart cart) {
        if (cart == null) {
            return CartResponse.builder().items(List.of()).totalItems(0).subtotalAmount(0L).build();
        }
        List<CartItemResponse> items = new ArrayList<>();
        long subtotal = 0L;
        int totalItems = 0;
        for (CartItem item : cart.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getVariant().getSku()))
                .toList()) {
            ProductVariant variant = item.getVariant();
            if (variant == null || !STATUS_ACTIVE.equalsIgnoreCase(variant.getStatus())
                    || variant.getProduct() == null
                    || !STATUS_ACTIVE.equalsIgnoreCase(variant.getProduct().getStatus())) {
                continue;
            }
            long lineTotal;
            try {
                lineTotal = Math.multiplyExact(variant.getListPrice(), (long) item.getQuantity());
                subtotal = Math.addExact(subtotal, lineTotal);
                totalItems = Math.addExact(totalItems, item.getQuantity());
            } catch (ArithmeticException ex) {
                throw new BusinessException(StatusCode.BAD_REQUEST, "Cart amount is too large");
            }
            items.add(CartItemResponse.builder()
                    .productVariantId(variant.getId())
                    .productId(variant.getProduct() == null ? null : variant.getProduct().getId())
                    .productName(variant.getProduct() == null ? "" : variant.getProduct().getName())
                    .sku(variant.getSku())
                    .model(variant.getModel())
                    .imageUrl(resolveMainImageUrl(variant))
                    .listPrice(variant.getListPrice())
                    .quantity(item.getQuantity())
                    .subtotal(lineTotal)
                    .stockQuantity(variant.getQuantity())
                    .build());
        }
        return CartResponse.builder().items(items).totalItems(totalItems).subtotalAmount(subtotal).build();
    }

    private String resolveMainImageUrl(ProductVariant variant) {
        return variant.getImages().stream()
                .filter(image -> image.isMain() && STATUS_ACTIVE.equalsIgnoreCase(image.getStatus()))
                .findFirst()
                .map(ProductImage::getFile)
                .map(File::getPublicUrl)
                .orElseGet(() -> variant.getImages().stream()
                        .filter(image -> STATUS_ACTIVE.equalsIgnoreCase(image.getStatus()))
                        .findFirst()
                        .map(ProductImage::getFile)
                        .map(File::getPublicUrl)
                        .orElse(null));
    }

    private void requireOwner(UUID accountId, String sessionToken) {
        requireOwner(accountId, sessionToken, true);
    }

    private void requireOwner(UUID accountId, String sessionToken, boolean required) {
        boolean accountOwner = accountId != null;
        boolean sessionOwner = sessionToken != null && !sessionToken.isBlank();
        if (required && accountOwner == sessionOwner) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Exactly one cart owner is required");
        }
    }

    private String normalizeSession(String sessionToken) {
        return sessionToken == null || sessionToken.isBlank() ? null : sessionToken.trim();
    }
}
