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
import com.ecm.server.model.DiscountProductVariant;
import com.ecm.server.repository.DiscountProductVariantRepository;
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
    public static final String SCOPE_PRODUCT = "PRODUCT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final int DEFAULT_LIMIT = 20;

    private final DiscountRepository discountRepository;
    private final DiscountProductVariantRepository discountProductVariantRepository;
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
        String typeFilter = (filter.getType() != null && !filter.getType().isBlank()) ? filter.getType().trim().toUpperCase() : null;
        String scopeFilter = (filter.getScope() != null && !filter.getScope().isBlank()) ? filter.getScope().trim().toUpperCase() : null;

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
        Discount discount = discountRepository.findByCodeIgnoreCase(request.getCode().trim())
                .orElseThrow(() -> new BusinessException(StatusCode.DISCOUNT_NOT_FOUND, "Discount code '" + request.getCode() + "' not found"));

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
        if (SCOPE_PRODUCT.equalsIgnoreCase(discount.getScope())) {
            List<DiscountProductVariant> dpvList = discountProductVariantRepository.findByDiscountIdAndStatus(discount.getId(), STATUS_ACTIVE);
            Set<UUID> eligibleVariantIds = dpvList.stream()
                    .map(dpv -> dpv.getProductVariant().getId())
                    .collect(Collectors.toSet());

            if (eligibleVariantIds.isEmpty()) {
                throw new BusinessException(StatusCode.BAD_REQUEST, "Discount is not applicable to any active products");
            }

            long matchingSubtotal = 0L;
            if (request.getItems() != null && !request.getItems().isEmpty()) {
                for (OrderItemValidateDto item : request.getItems()) {
                    if (eligibleVariantIds.contains(item.getProductVariantId())) {
                        matchingSubtotal += ((long) item.getQuantity() * item.getUnitPrice());
                    }
                }
            }

            if (matchingSubtotal <= 0) {
                throw new BusinessException(StatusCode.BAD_REQUEST, "Cart does not contain eligible products for this discount code");
            }
            eligibleAmount = matchingSubtotal;
        }

        // 6. Compute exact discount deduction amount
        int calculatedDiscountAmount = 0;
        if (TYPE_PERCENT.equalsIgnoreCase(discount.getType())) {
            calculatedDiscountAmount = (int) Math.round((eligibleAmount * discount.getValue()) / 100.0);
        } else if (TYPE_FIXED.equalsIgnoreCase(discount.getType())) {
            calculatedDiscountAmount = discount.getValue();
        }

        // Cap discount amount by eligible amount and total order amount
        calculatedDiscountAmount = (int) Math.min(calculatedDiscountAmount, eligibleAmount);
        calculatedDiscountAmount = (int) Math.min(calculatedDiscountAmount, totalOrderAmount);

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
