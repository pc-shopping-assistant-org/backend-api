package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateDiscountRequest;
import com.ecm.server.dto.request.DiscountFilterRequest;
import com.ecm.server.dto.request.UpdateDiscountRequest;
import com.ecm.server.dto.response.DiscountDetailResponse;
import com.ecm.server.dto.response.DiscountSummaryResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.DiscountMapper;
import com.ecm.server.model.Discount;
import com.ecm.server.model.DiscountProductVariant;
import com.ecm.server.model.Employee;
import com.ecm.server.model.ProductVariant;
import com.ecm.server.repository.*;
import com.ecm.server.service.AdminDiscountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDiscountServiceImpl implements AdminDiscountService {

    public static final String STATUS_DELETED = "DELETED";
    public static final int DEFAULT_LIMIT = 20;

    private final DiscountRepository discountRepository;
    private final DiscountProductVariantRepository discountProductVariantRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EmployeeRepository employeeRepository;
    private final DiscountMapper discountMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<DiscountSummaryResponse> getAdminDiscounts(DiscountFilterRequest filter) {
        // 1. Prepare pagination pageable and search parameters
        int pageSize = (filter.getLimit() != null && filter.getLimit() > 0) ? filter.getLimit() : DEFAULT_LIMIT;
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        String keywordPattern = (filter.getKeyword() != null && !filter.getKeyword().isBlank())
                ? "%" + filter.getKeyword().trim().toLowerCase() + "%"
                : null;
        String statusFilter = (filter.getStatus() != null && !filter.getStatus().isBlank()) ? filter.getStatus().trim().toUpperCase() : null;
        String typeFilter = (filter.getType() != null && !filter.getType().isBlank()) ? filter.getType().trim().toUpperCase() : null;
        String scopeFilter = (filter.getScope() != null && !filter.getScope().isBlank()) ? filter.getScope().trim().toUpperCase() : null;

        // 2. Query discounts using keyset pagination
        List<Discount> discounts = (filter.getCursor() == null)
                ? discountRepository.findAdminInitial(statusFilter, typeFilter, scopeFilter, keywordPattern, pageable)
                : discountRepository.findAdminAfterCursor(filter.getCursor(), statusFilter, typeFilter, scopeFilter, keywordPattern, pageable);

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
    public DiscountDetailResponse getDiscountById(UUID id) {
        // 1. Find discount with eager fetched applied variants
        Discount discount = discountRepository.findByIdWithDetails(id, STATUS_DELETED)
                .orElseThrow(() -> new BusinessException(StatusCode.DISCOUNT_NOT_FOUND));

        // 2. Map and return detail DTO
        return discountMapper.toDetailResponse(discount);
    }

    @Override
    @Transactional
    public DiscountDetailResponse createDiscount(CreateDiscountRequest request, UUID adminId) {
        // 1. Validate discount coupon code uniqueness
        String normalizedCode = request.getCode().trim().toUpperCase();
        if (discountRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new BusinessException(StatusCode.DISCOUNT_CODE_ALREADY_EXISTS, "Discount coupon code '" + normalizedCode + "' already exists");
        }

        // 2. Verify date range validity
        if (request.getEndAt().isBefore(request.getStartAt())) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Discount end date cannot be before start date");
        }

        // 3. Resolve audit employee ID and persist Discount entity
        UUID employeeId = resolveEmployeeId(adminId);
        Discount discount = discountMapper.toEntity(request);
        discount.setCode(normalizedCode);
        discount.setType(request.getType().toUpperCase());
        discount.setScope(request.getScope().toUpperCase());
        discount.setCreatedBy(employeeId);
        Discount savedDiscount = discountRepository.save(discount);

        // 4. Link applied product variants if provided
        if (request.getAppliedVariantIds() != null && !request.getAppliedVariantIds().isEmpty()) {
            List<DiscountProductVariant> dpvList = new ArrayList<>();
            for (UUID variantId : request.getAppliedVariantIds()) {
                ProductVariant variant = productVariantRepository.findById(variantId)
                        .filter(v -> !STATUS_DELETED.equalsIgnoreCase(v.getStatus()))
                        .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));
                DiscountProductVariant dpv = DiscountProductVariant.builder()
                        .discount(savedDiscount)
                        .productVariant(variant)
                        .status("ACTIVE")
                        .build();
                dpvList.add(discountProductVariantRepository.save(dpv));
            }
            savedDiscount.setDiscountProductVariants(new LinkedHashSet<>(dpvList));
        }

        // 5. Map and return created detail response DTO
        return discountMapper.toDetailResponse(savedDiscount);
    }

    @Override
    @Transactional
    public DiscountDetailResponse updateDiscount(UUID id, UpdateDiscountRequest request, UUID adminId) {
        // 1. Fetch existing discount entity
        Discount discount = discountRepository.findByIdWithDetails(id, STATUS_DELETED)
                .orElseThrow(() -> new BusinessException(StatusCode.DISCOUNT_NOT_FOUND));

        // 2. Verify date range validity
        if (request.getEndAt().isBefore(request.getStartAt())) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Discount end date cannot be before start date");
        }

        // 3. Update discount fields via MapStruct @MappingTarget
        UUID employeeId = resolveEmployeeId(adminId);
        discountMapper.updateEntityFromRequest(request, discount);
        discount.setType(request.getType().toUpperCase());
        discount.setScope(request.getScope().toUpperCase());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            discount.setStatus(request.getStatus().toUpperCase());
        }
        discount.setUpdatedBy(employeeId);

        // 4. Update applied product variant associations
        if (request.getAppliedVariantIds() != null) {
            discount.getDiscountProductVariants().clear();
            discountProductVariantRepository.deleteByDiscountId(id);
            discountProductVariantRepository.flush();

            List<DiscountProductVariant> newDpvList = new ArrayList<>();
            for (UUID variantId : request.getAppliedVariantIds()) {
                ProductVariant variant = productVariantRepository.findById(variantId)
                        .filter(v -> !STATUS_DELETED.equalsIgnoreCase(v.getStatus()))
                        .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));
                DiscountProductVariant dpv = DiscountProductVariant.builder()
                        .discount(discount)
                        .productVariant(variant)
                        .status("ACTIVE")
                        .build();
                newDpvList.add(discountProductVariantRepository.save(dpv));
            }
            discount.setDiscountProductVariants(new LinkedHashSet<>(newDpvList));
        }

        Discount updatedDiscount = discountRepository.save(discount);

        // 5. Assemble and return updated detail response DTO
        return discountMapper.toDetailResponse(updatedDiscount);
    }

    @Override
    @Transactional
    public void updateDiscountStatus(UUID id, String status, UUID adminId) {
        // 1. Retrieve discount entity
        Discount discount = discountRepository.findById(id)
                .filter(d -> !STATUS_DELETED.equalsIgnoreCase(d.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.DISCOUNT_NOT_FOUND));

        // 2. Update status and audit metadata
        UUID employeeId = resolveEmployeeId(adminId);
        discount.setStatus(status.toUpperCase());
        discount.setUpdatedBy(employeeId);
        discountRepository.save(discount);
        log.info("Updated discount [{}] status to [{}] by employee [{}]", id, status, employeeId);
    }

    @Override
    @Transactional
    public void deleteDiscount(UUID id) {
        // 1. Retrieve discount entity
        Discount discount = discountRepository.findById(id)
                .filter(d -> !STATUS_DELETED.equalsIgnoreCase(d.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.DISCOUNT_NOT_FOUND));

        // 2. Safety constraint check: prevent deletion if coupon has been applied in orders
        long orderCount = orderRepository.countByDiscountId(id);
        long orderItemCount = orderItemRepository.countByDiscountId(id);
        if (orderCount > 0 || orderItemCount > 0) {
            throw new BusinessException(StatusCode.CONFLICT, "Cannot delete discount coupon that has already been applied in existing orders");
        }

        // 3. Soft delete discount and unlink variant mappings
        discount.setStatus(STATUS_DELETED);
        discountRepository.save(discount);
        log.info("Soft deleted discount coupon with id: {}", id);
    }

    private UUID resolveEmployeeId(UUID accountId) {
        if (accountId == null) {
            return employeeRepository.findAll().stream().findFirst().map(Employee::getId).orElse(null);
        }
        return employeeRepository.findByAccountId(accountId)
                .map(Employee::getId)
                .or(() -> employeeRepository.findById(accountId).map(Employee::getId))
                .orElseGet(() -> employeeRepository.findAll().stream().findFirst().map(Employee::getId).orElse(null));
    }
}
