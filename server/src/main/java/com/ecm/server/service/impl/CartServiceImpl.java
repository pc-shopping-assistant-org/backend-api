package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.AddToCartRequest;
import com.ecm.server.dto.request.UpdateCartItemRequest;
import com.ecm.server.dto.response.CartItemResponse;
import com.ecm.server.dto.response.CartResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.model.ProductVariant;
import com.ecm.server.repository.ProductVariantRepository;
import com.ecm.server.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    public static final String CART_KEY_PREFIX = "cart:";
    public static final String POLICY_DENY = "DENY";
    public static final String STATUS_ACTIVE = "ACTIVE";

    private final StringRedisTemplate redisTemplate;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID accountId) {
        // 1. Construct Redis cart key
        String cartKey = buildCartKey(accountId);

        // 2. Fetch all cart items from Redis hash
        Map<Object, Object> rawEntries = redisTemplate.opsForHash().entries(cartKey);
        if (rawEntries == null || rawEntries.isEmpty()) {
            return CartResponse.builder()
                    .items(List.of())
                    .totalItems(0)
                    .subtotalAmount(0L)
                    .build();
        }

        // 3. Enrich items with real-time product variant data from database
        List<CartItemResponse> items = new ArrayList<>();
        long subtotalAmount = 0L;
        int totalItems = 0;

        for (Map.Entry<Object, Object> entry : rawEntries.entrySet()) {
            UUID variantId = UUID.fromString(entry.getKey().toString());
            int quantity = Integer.parseInt(entry.getValue().toString());

            var variantOpt = productVariantRepository.findByIdWithProduct(variantId)
                    .filter(v -> STATUS_ACTIVE.equalsIgnoreCase(v.getStatus()));

            if (variantOpt.isEmpty()) {
                // Remove inactive or deleted variant from Redis cart
                redisTemplate.opsForHash().delete(cartKey, entry.getKey().toString());
                continue;
            }

            ProductVariant variant = variantOpt.get();
            int unitPrice = (variant.getPriceSale() != null && variant.getPriceSale() > 0) ? variant.getPriceSale() : variant.getPrice();
            long lineTotal = (long) unitPrice * quantity;
            subtotalAmount += lineTotal;
            totalItems += quantity;

            items.add(CartItemResponse.builder()
                    .productVariantId(variant.getId())
                    .productId(variant.getProduct() != null ? variant.getProduct().getId() : null)
                    .productName(variant.getProduct() != null ? variant.getProduct().getName() : "")
                    .sku(variant.getSku())
                    .model(variant.getModel())
                    .imageUrl(variant.getImageUrl())
                    .price(variant.getPrice())
                    .priceSale(variant.getPriceSale())
                    .quantity(quantity)
                    .subtotal(lineTotal)
                    .stockQuantity(variant.getQuantity())
                    .build());
        }

        // 4. Assemble and return CartResponse
        return CartResponse.builder()
                .items(items)
                .totalItems(totalItems)
                .subtotalAmount(subtotalAmount)
                .build();
    }

    @Override
    @Transactional
    public CartResponse addToCart(UUID accountId, AddToCartRequest request) {
        // 1. Validate product variant existence and active status
        ProductVariant variant = productVariantRepository.findByIdWithProduct(request.getProductVariantId())
                .filter(v -> STATUS_ACTIVE.equalsIgnoreCase(v.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));

        // 2. Fetch existing quantity from Redis
        String cartKey = buildCartKey(accountId);
        String variantIdStr = request.getProductVariantId().toString();
        Object currentQtyObj = redisTemplate.opsForHash().get(cartKey, variantIdStr);
        int currentQty = currentQtyObj != null ? Integer.parseInt(currentQtyObj.toString()) : 0;
        int newQty = currentQty + request.getQuantity();

        // 3. Enforce inventory stock constraint
        if (POLICY_DENY.equalsIgnoreCase(variant.getInventoryPolicy()) && variant.getQuantity() < newQty) {
            throw new BusinessException(StatusCode.INSUFFICIENT_STOCK, "Requested quantity exceeds available stock (" + variant.getQuantity() + ")");
        }

        // 4. Update Redis cart hash
        redisTemplate.opsForHash().put(cartKey, variantIdStr, String.valueOf(newQty));
        log.info("Added variant [{}] qty [{}] to cart for user [{}]", request.getProductVariantId(), request.getQuantity(), accountId);

        // 5. Return latest enriched cart
        return getCart(accountId);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(UUID accountId, UUID variantId, UpdateCartItemRequest request) {
        // 1. Validate product variant
        ProductVariant variant = productVariantRepository.findByIdWithProduct(variantId)
                .filter(v -> STATUS_ACTIVE.equalsIgnoreCase(v.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));

        String cartKey = buildCartKey(accountId);
        String variantIdStr = variantId.toString();

        // 2. Handle item removal if quantity is zero
        if (request.getQuantity() <= 0) {
            redisTemplate.opsForHash().delete(cartKey, variantIdStr);
            log.info("Removed variant [{}] from cart for user [{}]", variantId, accountId);
            return getCart(accountId);
        }

        // 3. Enforce inventory stock check
        if (POLICY_DENY.equalsIgnoreCase(variant.getInventoryPolicy()) && variant.getQuantity() < request.getQuantity()) {
            throw new BusinessException(StatusCode.INSUFFICIENT_STOCK, "Requested quantity exceeds available stock (" + variant.getQuantity() + ")");
        }

        // 4. Update quantity in Redis
        redisTemplate.opsForHash().put(cartKey, variantIdStr, String.valueOf(request.getQuantity()));
        log.info("Updated variant [{}] qty [{}] in cart for user [{}]", variantId, request.getQuantity(), accountId);

        // 5. Return updated cart
        return getCart(accountId);
    }

    @Override
    public CartResponse removeCartItem(UUID accountId, UUID variantId) {
        // 1. Remove variant entry from Redis cart hash
        String cartKey = buildCartKey(accountId);
        redisTemplate.opsForHash().delete(cartKey, variantId.toString());
        log.info("Removed variant [{}] from cart for user [{}]", variantId, accountId);

        // 2. Return updated cart response
        return getCart(accountId);
    }

    @Override
    public void clearCart(UUID accountId) {
        // 1. Delete complete cart key in Redis
        String cartKey = buildCartKey(accountId);
        redisTemplate.delete(cartKey);
        log.info("Cleared cart for user [{}]", accountId);
    }

    private String buildCartKey(UUID accountId) {
        return CART_KEY_PREFIX + accountId;
    }
}
