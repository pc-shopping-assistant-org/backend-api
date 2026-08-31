package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateProductImageRequest;
import com.ecm.server.dto.request.CreateProductVariantRequest;
import com.ecm.server.dto.request.UpdateProductVariantRequest;
import com.ecm.server.dto.response.ProductImageResponse;
import com.ecm.server.dto.response.ProductVariantResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.ProductImageMapper;
import com.ecm.server.mapper.ProductVariantMapper;
import com.ecm.server.model.*;
import com.ecm.server.repository.*;
import com.ecm.server.service.AdminProductVariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductVariantServiceImpl implements AdminProductVariantService {

    public static final String STATUS_DELETED = "DELETED";

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final FileRepository fileRepository;
    private final OptionRepository optionRepository;
    private final VariantOptionRepository variantOptionRepository;
    private final OrderItemRepository orderItemRepository;
    private final com.ecm.server.repository.EmployeeRepository employeeRepository;
    private final ProductVariantMapper productVariantMapper;
    private final ProductImageMapper productImageMapper;

    @Override
    @Transactional
    public ProductVariantResponse createVariant(UUID productId, CreateProductVariantRequest request, UUID adminId) {
        // 1. Verify parent product existence
        Product product = productRepository.findById(productId)
                .filter(p -> !STATUS_DELETED.equalsIgnoreCase(p.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_NOT_FOUND));

        // 2. Validate SKU and barcode uniqueness
        if (productVariantRepository.existsBySku(request.getSku())) {
            throw new BusinessException(StatusCode.SKU_ALREADY_EXISTS, "SKU '" + request.getSku() + "' already exists");
        }
        if (request.getBarcode() != null && !request.getBarcode().isBlank()
                && productVariantRepository.existsByBarcode(request.getBarcode())) {
            throw new BusinessException(StatusCode.CONFLICT, "Barcode '" + request.getBarcode() + "' already exists");
        }

        // 3. Map DTO to entity via MapStruct and persist variant
        UUID employeeId = resolveEmployeeId(adminId);
        ProductVariant variant = productVariantMapper.toEntity(request);
        variant.setProduct(product);
        variant.setCreatedBy(employeeId);
        ProductVariant savedVariant = productVariantRepository.save(variant);

        // 4. Link variant options
        if (request.getOptionIds() != null && !request.getOptionIds().isEmpty()) {
            validateOptionTypes(request.getOptionIds());
            List<VariantOption> variantOptions = new ArrayList<>();
            for (UUID optionId : request.getOptionIds()) {
                Option option = optionRepository.findById(optionId)
                        .filter(o -> "ACTIVE".equalsIgnoreCase(o.getStatus()))
                        .orElseThrow(() -> new BusinessException(StatusCode.OPTION_NOT_FOUND));
                VariantOption vo = VariantOption.builder()
                        .productVariant(savedVariant)
                        .option(option)
                        .build();
                variantOptions.add(variantOptionRepository.save(vo));
            }
            savedVariant.setVariantOptions(new java.util.LinkedHashSet<>(variantOptions));
        }

        // 5. Save variant images
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            validateMainImages(request.getImages());
            List<ProductImage> productImages = new ArrayList<>();
            for (CreateProductImageRequest imgReq : request.getImages()) {
                        demoteExistingMainImage(savedVariant, imgReq);
                        ProductImage img = productImageMapper.toEntity(imgReq);
                        img.setProductVariant(savedVariant);
                        img.setFile(resolveFile(imgReq));
                        productImages.add(productImageRepository.save(img));
            }
            savedVariant.setImages(new java.util.LinkedHashSet<>(productImages));
        }

        // 6. Return populated variant response DTO
        return productVariantMapper.toResponse(savedVariant);
    }

    @Override
    @Transactional
    public ProductVariantResponse updateVariant(UUID variantId, UpdateProductVariantRequest request, UUID adminId) {
        // 1. Fetch existing variant
        ProductVariant variant = productVariantRepository.findByIdWithDetails(variantId, STATUS_DELETED)
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));

        // Deletion is guarded by deleteVariant() so order history cannot be
        // bypassed through the generic update request.
        validateMutableStatus(request.getStatus());

        // 2. Validate barcode uniqueness if modified
        if (request.getBarcode() != null && !request.getBarcode().isBlank()
                && !request.getBarcode().equalsIgnoreCase(variant.getBarcode())
                && productVariantRepository.existsByBarcode(request.getBarcode())) {
            throw new BusinessException(StatusCode.CONFLICT, "Barcode '" + request.getBarcode() + "' already exists");
        }

        // 3. Update variant attributes via MapStruct @MappingTarget
        UUID employeeId = resolveEmployeeId(adminId);
        productVariantMapper.updateEntityFromRequest(request, variant);
        variant.setUpdatedBy(employeeId);

        // 4. Update option associations if provided
        if (request.getOptionIds() != null) {
            validateOptionTypes(request.getOptionIds());
            variant.getVariantOptions().clear();
            variantOptionRepository.deleteByProductVariantId(variantId);
            variantOptionRepository.flush();
            List<VariantOption> newOptions = new ArrayList<>();
            for (UUID optionId : request.getOptionIds()) {
                Option option = optionRepository.findById(optionId)
                        .filter(o -> "ACTIVE".equalsIgnoreCase(o.getStatus()))
                        .orElseThrow(() -> new BusinessException(StatusCode.OPTION_NOT_FOUND));
                VariantOption vo = VariantOption.builder()
                        .productVariant(variant)
                        .option(option)
                        .build();
                newOptions.add(variantOptionRepository.save(vo));
            }
            variant.setVariantOptions(new java.util.LinkedHashSet<>(newOptions));
        }

        ProductVariant updatedVariant = productVariantRepository.save(variant);

        // 5. Return updated variant response DTO
        return productVariantMapper.toResponse(updatedVariant);
    }

    @Override
    @Transactional
    public void deleteVariant(UUID variantId) {
        // 1. Retrieve variant entity
        ProductVariant variant = productVariantRepository.findById(variantId)
                .filter(v -> !STATUS_DELETED.equalsIgnoreCase(v.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));

        // 2. Enforce safety constraint: check if variant is part of customer orders
        long orderCount = orderItemRepository.countByProductVariantId(variantId);
        if (orderCount > 0) {
            throw new BusinessException(StatusCode.CONFLICT, "Cannot delete product variant with existing orders");
        }

        // 3. Perform soft delete
        variant.setStatus(STATUS_DELETED);
        productVariantRepository.save(variant);
        log.info("Soft deleted variant with id: {}", variantId);
    }

    @Override
    @Transactional
    public ProductImageResponse addVariantImage(UUID variantId, CreateProductImageRequest request) {
        // 1. Validate variant existence
        ProductVariant variant = productVariantRepository.findById(variantId)
                .filter(v -> !STATUS_DELETED.equalsIgnoreCase(v.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));

        // 2. Persist new image entity
        ProductImage image = productImageMapper.toEntity(request);
        image.setProductVariant(variant);
        demoteExistingMainImage(variant, request);
        image.setFile(resolveFile(request));
        ProductImage savedImage = productImageRepository.save(image);

        // 3. Return created image response DTO
        return productImageMapper.toResponse(savedImage);
    }

    @Override
    @Transactional
    public void deleteImage(UUID imageId) {
        // 1. Retrieve image entity
        ProductImage image = productImageRepository.findById(imageId)
                .filter(img -> !STATUS_DELETED.equalsIgnoreCase(img.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.IMAGE_NOT_FOUND));

        // 2. Soft delete image
        image.setStatus(STATUS_DELETED);
        productImageRepository.save(image);
        log.info("Soft deleted image with id: {}", imageId);
    }

    private UUID resolveEmployeeId(UUID accountId) {
        if (accountId == null) {
            return employeeRepository.findAll().stream().findFirst().map(com.ecm.server.model.Employee::getAccountId).orElse(null);
        }
        return employeeRepository.findByAccountId(accountId)
                .map(com.ecm.server.model.Employee::getAccountId)
                .or(() -> employeeRepository.findById(accountId).map(com.ecm.server.model.Employee::getAccountId))
                .orElseGet(() -> employeeRepository.findAll().stream().findFirst().map(com.ecm.server.model.Employee::getAccountId).orElse(null));
    }

    private com.ecm.server.model.File resolveFile(CreateProductImageRequest request) {
        return fileRepository.findById(request.getFileId())
                .filter(file -> "ACTIVE".equalsIgnoreCase(file.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.IMAGE_NOT_FOUND, "Referenced file was not found"));
    }

    private void validateOptionTypes(List<UUID> optionIds) {
        Set<String> types = new HashSet<>();
        for (UUID optionId : optionIds) {
            Option option = optionRepository.findById(optionId)
                    .filter(o -> "ACTIVE".equalsIgnoreCase(o.getStatus()))
                    .orElseThrow(() -> new BusinessException(StatusCode.OPTION_NOT_FOUND));
            if (!types.add(option.getType().trim().toUpperCase())) {
                throw new BusinessException(StatusCode.BAD_REQUEST,
                        "A variant cannot contain more than one option of the same type");
            }
        }
    }

    private void validateMutableStatus(String status) {
        if (status != null && !"ACTIVE".equalsIgnoreCase(status)
                && !"INACTIVE".equalsIgnoreCase(status)) {
            throw new BusinessException(StatusCode.BAD_REQUEST,
                    "Only ACTIVE or INACTIVE is allowed here; use the delete endpoint for DELETED");
        }
    }

    private void demoteExistingMainImage(ProductVariant variant, CreateProductImageRequest request) {
        if (!Boolean.TRUE.equals(request.getIsMain())) {
            return;
        }
        productImageRepository.findByProductVariantIdAndStatusNot(variant.getId(), STATUS_DELETED)
                .stream()
                .filter(ProductImage::isMain)
                .forEach(existing -> {
                    existing.setMain(false);
                    productImageRepository.save(existing);
                });
    }

    private void validateMainImages(List<CreateProductImageRequest> images) {
        long mainCount = images.stream().filter(image -> Boolean.TRUE.equals(image.getIsMain())).count();
        if (mainCount > 1) {
            throw new BusinessException(StatusCode.BAD_REQUEST,
                    "A product variant can have at most one main image");
        }
    }
}
