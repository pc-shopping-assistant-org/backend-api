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
import com.ecm.server.model.DiscountCategory;
import com.ecm.server.model.DiscountVariant;
import com.ecm.server.model.Category;
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
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDiscountServiceImpl implements AdminDiscountService {

    public static final String STATUS_DELETED = "DELETED";
    public static final int DEFAULT_LIMIT = 20;

    private final DiscountRepository discountRepository;
    private final DiscountVariantRepository discountVariantRepository;
    private final DiscountCategoryRepository discountCategoryRepository;
    private final CategoryRepository categoryRepository;
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
        String typeFilter = (filter.getDiscountType() != null && !filter.getDiscountType().isBlank()) ? filter.getDiscountType().trim().toUpperCase() : null;
        String scopeFilter = (filter.getApplicationScope() != null && !filter.getApplicationScope().isBlank()) ? filter.getApplicationScope().trim().toUpperCase() : null;

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
        discount.setDiscountCategories(new LinkedHashSet<>(discountCategoryRepository.findByDiscountIdDiscountId(id)));

        // 2. Map and return detail DTO
        return discountMapper.toDetailResponse(discount);
    }

    @Override
    @Transactional
    public DiscountDetailResponse createDiscount(CreateDiscountRequest request, UUID adminId) {
        // 1. Validate discount coupon code uniqueness
        String normalizedCode = request.getCode() == null || request.getCode().isBlank()
                ? null : request.getCode().trim().toUpperCase();
        if (normalizedCode != null && discountRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new BusinessException(StatusCode.DISCOUNT_CODE_ALREADY_EXISTS, "Discount coupon code '" + normalizedCode + "' already exists");
        }

        // 2. Verify date range validity
        if (!request.getEndAt().isAfter(request.getStartAt())) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Discount end date must be after start date");
        }
        validateTypeValue(request.getDiscountType(), request.getValue());
        validateCodeScope(normalizedCode, request.getApplicationScope());

        // 3. Resolve audit employee ID and persist Discount entity
        UUID employeeId = resolveEmployeeId(adminId);
        Discount discount = discountMapper.toEntity(request);
        discount.setCode(normalizedCode);
        discount.setDiscountType(request.getDiscountType().toUpperCase());
        discount.setApplicationScope(request.getApplicationScope().toUpperCase());
        discount.setCreatedBy(employeeId);
        validateTargets(request.getApplicationScope(), request.getAppliedVariantIds(), request.getAppliedCategoryIds());
        Discount savedDiscount = discountRepository.save(discount);

        // 4. Link normalized category and variant targets.
        if (request.getAppliedVariantIds() != null && !request.getAppliedVariantIds().isEmpty()) {
            List<DiscountVariant> targets = new ArrayList<>();
            for (UUID variantId : request.getAppliedVariantIds()) {
                ProductVariant variant = productVariantRepository.findById(variantId)
                        .filter(v -> "ACTIVE".equalsIgnoreCase(v.getStatus()))
                        .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));
                DiscountVariant target = DiscountVariant.builder()
                        .id(new DiscountVariant.DiscountVariantId(savedDiscount.getId(), variant.getId()))
                        .discount(savedDiscount)
                        .variant(variant)
                        .build();
                targets.add(discountVariantRepository.save(target));
            }
            savedDiscount.setDiscountVariants(new LinkedHashSet<>(targets));
        }
        if (request.getAppliedCategoryIds() != null && !request.getAppliedCategoryIds().isEmpty()) {
            List<DiscountCategory> categoryTargets = new ArrayList<>();
            for (UUID categoryId : request.getAppliedCategoryIds()) {
                Category category = categoryRepository.findById(categoryId)
                        .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                        .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_NOT_FOUND));
                categoryTargets.add(DiscountCategory.builder()
                        .id(new DiscountCategory.DiscountCategoryId(savedDiscount.getId(), category.getId()))
                        .discount(savedDiscount)
                        .category(category)
                        .build());
            }
            categoryTargets.forEach(discountCategoryRepository::save);
            savedDiscount.setDiscountCategories(new LinkedHashSet<>(categoryTargets));
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

        // Deletion has a separate guard that protects order snapshots.  A
        // generic edit may hide/expire a discount, but may not soft-delete it.
        validateMutableStatus(request.getStatus());

        // 2. Verify date range validity
        if (!request.getEndAt().isAfter(request.getStartAt())) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Discount end date must be after start date");
        }
        validateTypeValue(request.getDiscountType(), request.getValue());
        String normalizedCode = request.getCode() == null
                ? discount.getCode()
                : request.getCode().isBlank() ? null : request.getCode().trim().toUpperCase();
        if (normalizedCode != null
                && !normalizedCode.equalsIgnoreCase(discount.getCode())
                && discountRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new BusinessException(StatusCode.DISCOUNT_CODE_ALREADY_EXISTS,
                    "Discount coupon code '" + normalizedCode + "' already exists");
        }

        // 3. Update discount fields via MapStruct @MappingTarget
        UUID employeeId = resolveEmployeeId(adminId);
        discountMapper.updateEntityFromRequest(request, discount);
        discount.setCode(normalizedCode);
        discount.setDiscountType(request.getDiscountType().toUpperCase());
        discount.setApplicationScope(request.getApplicationScope().toUpperCase());
        validateCodeScope(normalizedCode, request.getApplicationScope());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            discount.setStatus(request.getStatus().toUpperCase());
        }
        discount.setUpdatedBy(employeeId);

        // 4. Update applied product variant associations
        // Target lists are a complete replacement on update. Treat omitted
        // lists as empty, otherwise changing a scoped discount to ORDER or
        // ALL_ITEMS would leave stale normalized target rows behind.
        List<UUID> requestedVariantIds = request.getAppliedVariantIds() == null
                ? List.of() : request.getAppliedVariantIds();
        List<UUID> requestedCategoryIds = request.getAppliedCategoryIds() == null
                ? List.of() : request.getAppliedCategoryIds();
        validateTargets(request.getApplicationScope(), requestedVariantIds, requestedCategoryIds);
        {
            discount.getDiscountVariants().clear();
            discount.getDiscountCategories().clear();
            discountVariantRepository.deleteByDiscountId(id);
            discountCategoryRepository.deleteByDiscountId(id);
            discountVariantRepository.flush();

            List<DiscountVariant> newTargets = new ArrayList<>();
            for (UUID variantId : requestedVariantIds) {
                ProductVariant variant = productVariantRepository.findById(variantId)
                        .filter(v -> "ACTIVE".equalsIgnoreCase(v.getStatus()))
                        .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));
                DiscountVariant target = DiscountVariant.builder()
                        .id(new DiscountVariant.DiscountVariantId(discount.getId(), variant.getId()))
                        .discount(discount)
                        .variant(variant)
                        .build();
                newTargets.add(discountVariantRepository.save(target));
            }
            discount.setDiscountVariants(new LinkedHashSet<>(newTargets));
            for (UUID categoryId : requestedCategoryIds) {
                Category category = categoryRepository.findById(categoryId)
                        .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                        .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_NOT_FOUND));
                discountCategoryRepository.save(DiscountCategory.builder()
                        .id(new DiscountCategory.DiscountCategoryId(discount.getId(), category.getId()))
                        .discount(discount)
                        .category(category)
                        .build());
            }
            discount.setDiscountCategories(new LinkedHashSet<>(discountCategoryRepository.findByDiscountIdDiscountId(id)));
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

        // 2. Hide/expire/disable only.  DELETED must go through deleteDiscount(),
        // which checks historical order usage.
        validateMutableStatus(status);

        // 3. Update status and audit metadata
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
        long orderCount = orderRepository.countByOrderDiscountId(id);
        long orderItemCount = orderItemRepository.countByItemDiscountRelationId(id);
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
            return employeeRepository.findAll().stream().findFirst().map(Employee::getAccountId).orElse(null);
        }
        return employeeRepository.findByAccountId(accountId)
                .map(Employee::getAccountId)
                .or(() -> employeeRepository.findById(accountId).map(Employee::getAccountId))
                .orElseGet(() -> employeeRepository.findAll().stream().findFirst().map(Employee::getAccountId).orElse(null));
    }

    private void validateTargets(String scope, List<UUID> variantIds, List<UUID> categoryIds) {
        String normalizedScope = scope == null ? "" : scope.trim().toUpperCase();
        int variantCount = variantIds == null ? 0 : variantIds.stream().filter(java.util.Objects::nonNull).distinct().toList().size();
        int categoryCount = categoryIds == null ? 0 : categoryIds.stream().filter(java.util.Objects::nonNull).distinct().toList().size();
        if ((variantIds != null && variantIds.size() != variantCount)
                || (categoryIds != null && categoryIds.size() != categoryCount)) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Discount target IDs must be unique");
        }
        boolean valid = switch (normalizedScope) {
            case "ORDER", "ALL_ITEMS" -> variantCount == 0 && categoryCount == 0;
            case "CATEGORY" -> categoryCount > 0 && variantCount == 0;
            case "VARIANT" -> variantCount > 0 && categoryCount == 0;
            default -> false;
        };
        if (!valid) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Discount targets do not match application scope");
        }
    }

    private void validateTypeValue(String type, Integer value) {
        if (type == null || value == null) {
            return;
        }
        boolean valid = "PERCENT".equalsIgnoreCase(type)
                ? value > 0 && value <= 100
                : "FIXED".equalsIgnoreCase(type) && value > 0;
        if (!valid) {
            throw new BusinessException(StatusCode.BAD_REQUEST,
                    "PERCENT value must be between 1 and 100; FIXED value must be greater than 0");
        }
    }

    private void validateCodeScope(String code, String scope) {
        if (code != null && !code.isBlank() && !"ORDER".equalsIgnoreCase(scope)) {
            throw new BusinessException(StatusCode.BAD_REQUEST,
                    "Only ORDER discounts may have a voucher code");
        }
    }

    private void validateMutableStatus(String status) {
        if (status == null || status.isBlank()) {
            return;
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("ACTIVE", "INACTIVE", "EXPIRED", "DISABLED").contains(normalized)) {
            throw new BusinessException(StatusCode.BAD_REQUEST,
                    "Only ACTIVE, INACTIVE, EXPIRED, or DISABLED is allowed here; use the delete endpoint for DELETED");
        }
    }
}
