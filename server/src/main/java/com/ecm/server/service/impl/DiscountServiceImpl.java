package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.DiscountFilterRequest;
import com.ecm.server.dto.request.OrderItemValidateDto;
import com.ecm.server.dto.request.ValidateDiscountRequest;
import com.ecm.server.dto.response.DiscountSummaryResponse;
import com.ecm.server.dto.response.DiscountValidationResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.DiscountMapper;
import com.ecm.server.model.Discount;
import com.ecm.server.model.DiscountCategory;
import com.ecm.server.model.DiscountVariant;
import com.ecm.server.model.ProductVariant;
import com.ecm.server.repository.DiscountCategoryRepository;
import com.ecm.server.repository.DiscountVariantRepository;
import com.ecm.server.repository.ProductVariantRepository;
import com.ecm.server.repository.DiscountRepository;
import com.ecm.server.service.DiscountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {

    public static final String TYPE_PERCENT = "PERCENT";
    public static final String TYPE_FIXED = "FIXED";
    public static final String SCOPE_VARIANT = "VARIANT";
    public static final String SCOPE_CATEGORY = "CATEGORY";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final int DEFAULT_LIMIT = 20;

    private final DiscountRepository discountRepository;
    private final DiscountVariantRepository discountVariantRepository;
    private final DiscountCategoryRepository discountCategoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final DiscountMapper discountMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<DiscountSummaryResponse> getActiveDiscounts(DiscountFilterRequest filter) {
        // 1. Prepare pagination pageable and search parameters
        int pageSize = (filter.getLimit() != null && filter.getLimit() > 0) ? filter.getLimit() : DEFAULT_LIMIT;
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        Instant now = Instant.now();
        String keywordPattern = (filter.getKeyword() != null && !filter.getKeyword().isBlank())
                ? "%" + filter.getKeyword().trim().toLowerCase() + "%"
                : null;
        String typeFilter = (filter.getDiscountType() != null && !filter.getDiscountType().isBlank()) ? filter.getDiscountType().trim().toUpperCase() : null;
        String scopeFilter = (filter.getApplicationScope() != null && !filter.getApplicationScope().isBlank()) ? filter.getApplicationScope().trim().toUpperCase() : null;

        // 2. Fetch active discounts using keyset cursor pagination
        List<Discount> discounts = (filter.getCursor() == null)
                ? discountRepository.findActiveInitial(now, typeFilter, scopeFilter, keywordPattern, pageable)
                : discountRepository.findActiveAfterCursor(filter.getCursor(), now, typeFilter, scopeFilter, keywordPattern, pageable);

        // 3. Assemble and return cursor response envelope
        return CursorPageResponse.of(
                discounts,
                pageSize,
                discount -> discount.getId().toString(),
                discountMapper::toSummaryResponse
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountValidationResponse validateDiscount(ValidateDiscountRequest request) {
        // 1. Find discount by coupon code (case-insensitive)
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BusinessException(StatusCode.DISCOUNT_NOT_FOUND);
        }
        String normalizedCode = request.getCode().trim().toUpperCase(java.util.Locale.ROOT);
        Discount discount = discountRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new BusinessException(StatusCode.DISCOUNT_NOT_FOUND, "Discount code '" + request.getCode() + "' not found"));

        // A code is an order voucher. Automatic item promotions intentionally
        // have no code and are selected by checkout per order line.
        if (!"ORDER".equalsIgnoreCase(discount.getApplicationScope())) {
            throw new BusinessException(StatusCode.BAD_REQUEST,
                    "Only ORDER discounts can be applied with a voucher code");
        }

        // 2. Verify active status
        if (!STATUS_ACTIVE.equalsIgnoreCase(discount.getStatus())) {
            throw new BusinessException(StatusCode.DISCOUNT_EXPIRED, "Discount code is no longer active");
        }

        // 3. Verify validity date window (startAt <= now <= endAt)
        Instant now = Instant.now();
        if (now.isBefore(discount.getStartAt()) || now.isAfter(discount.getEndAt())) {
            throw new BusinessException(StatusCode.DISCOUNT_EXPIRED, "Discount code has expired or is not yet effective");
        }

        // 4. Verify minimum order threshold requirement
        long totalOrderAmount = request.getOrderAmount() != null ? request.getOrderAmount() : 0L;
        if (totalOrderAmount < discount.getMinOrderAmount()) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Order amount of " + totalOrderAmount + " does not meet the minimum required " + discount.getMinOrderAmount());
        }

        // 5. Verify scope and calculate eligible subtotal amount
        long eligibleAmount = totalOrderAmount;
        String scope = discount.getApplicationScope();
        if (SCOPE_VARIANT.equalsIgnoreCase(scope) || SCOPE_CATEGORY.equalsIgnoreCase(scope)) {
            Set<UUID> eligibleVariantIds;
            if (SCOPE_VARIANT.equalsIgnoreCase(scope)) {
                eligibleVariantIds = discountVariantRepository.findByDiscountIdDiscountId(discount.getId()).stream()
                        .map(DiscountVariant::getVariant)
                        .filter(java.util.Objects::nonNull)
                        .filter(v -> STATUS_ACTIVE.equalsIgnoreCase(v.getStatus())
                                && v.getProduct() != null
                                && STATUS_ACTIVE.equalsIgnoreCase(v.getProduct().getStatus())
                                && v.getProduct().getCategory() != null
                                && STATUS_ACTIVE.equalsIgnoreCase(v.getProduct().getCategory().getStatus()))
                        .map(v -> v.getId())
                        .collect(Collectors.toSet());
            } else {
                List<DiscountCategory> targets = discountCategoryRepository.findByDiscountIdDiscountId(discount.getId());
                Set<UUID> categoryIds = targets.stream().map(t -> t.getCategory().getId()).collect(Collectors.toSet());
                eligibleVariantIds = productVariantRepository.findIdsByProductCategoryIds(categoryIds).stream().collect(Collectors.toSet());
            }

            if (eligibleVariantIds.isEmpty()) {
                throw new BusinessException(StatusCode.BAD_REQUEST, "Discount is not applicable to any active products");
            }

            long matchingSubtotal = 0L;
            Set<UUID> seenVariants = new java.util.HashSet<>();
            if (request.getItems() != null && !request.getItems().isEmpty()) {
                for (OrderItemValidateDto item : request.getItems()) {
                    if (!seenVariants.add(item.getProductVariantId())) {
                        throw new BusinessException(StatusCode.BAD_REQUEST,
                                "A product variant may appear only once when validating a discount");
                    }
                    if (eligibleVariantIds.contains(item.getProductVariantId())) {
                        ProductVariant canonicalVariant = productVariantRepository.findByIdWithProduct(item.getProductVariantId())
                                .filter(v -> STATUS_ACTIVE.equalsIgnoreCase(v.getStatus())
                                        && v.getProduct() != null
                                        && STATUS_ACTIVE.equalsIgnoreCase(v.getProduct().getStatus()))
                                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));
                        try {
                            matchingSubtotal = Math.addExact(matchingSubtotal,
                                    Math.multiplyExact(canonicalVariant.getListPrice(), item.getQuantity().longValue()));
                        } catch (ArithmeticException ex) {
                            throw new BusinessException(StatusCode.BAD_REQUEST, "Discount validation amount is too large");
                        }
                    }
                }
            }

            if (matchingSubtotal <= 0) {
                throw new BusinessException(StatusCode.BAD_REQUEST, "Cart does not contain eligible products for this discount code");
            }
            eligibleAmount = matchingSubtotal;
        }

        // 6. Compute exact discount deduction amount
        long calculatedDiscountAmount = 0L;
        if (TYPE_PERCENT.equalsIgnoreCase(discount.getDiscountType())) {
            calculatedDiscountAmount = Math.round((eligibleAmount * discount.getValue()) / 100.0);
        } else if (TYPE_FIXED.equalsIgnoreCase(discount.getDiscountType())) {
            calculatedDiscountAmount = discount.getValue();
        }

        // Cap discount amount by eligible amount and total order amount
        calculatedDiscountAmount = Math.min(calculatedDiscountAmount, eligibleAmount);
        calculatedDiscountAmount = Math.min(calculatedDiscountAmount, totalOrderAmount);

        long finalAmount = Math.max(0L, totalOrderAmount - calculatedDiscountAmount);

        // 7. Return validation response DTO
        return DiscountValidationResponse.builder()
                .isValid(true)
                .discountId(discount.getId())
                .code(discount.getCode())
                .title(discount.getTitle())
                .discountAmount(calculatedDiscountAmount)
                .finalAmount(finalAmount)
                .message("Discount code applied successfully")
                .build();
    }
}
