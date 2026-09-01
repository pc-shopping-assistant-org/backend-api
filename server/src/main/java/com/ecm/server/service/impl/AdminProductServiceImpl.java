package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.*;
import com.ecm.server.dto.response.ProductDetailResponse;
import com.ecm.server.dto.response.ProductSummaryResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.ProductImageMapper;
import com.ecm.server.mapper.ProductMapper;
import com.ecm.server.mapper.ProductVariantMapper;
import com.ecm.server.mapper.SupplierMapper;
import com.ecm.server.model.*;
import com.ecm.server.repository.*;
import com.ecm.server.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
public class AdminProductServiceImpl implements AdminProductService {

    public static final String STATUS_DELETED = "DELETED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final int DEFAULT_LIMIT = 20;

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final FileRepository fileRepository;
    private final OptionRepository optionRepository;
    private final VariantOptionRepository variantOptionRepository;
    private final OrderItemRepository orderItemRepository;
    private final com.ecm.server.repository.EmployeeRepository employeeRepository;
    private final ProductMapper productMapper;
    private final ProductVariantMapper productVariantMapper;
    private final ProductImageMapper productImageMapper;
    private final SupplierMapper supplierMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<ProductSummaryResponse> getAdminProducts(ProductFilterRequest filter) {
        validatePriceRange(filter);
        // 1. Prepare pagination pageable and search parameters
        int pageSize = (filter.getLimit() != null && filter.getLimit() > 0) ? filter.getLimit() : DEFAULT_LIMIT;
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        String keywordPattern = (filter.getKeyword() != null && !filter.getKeyword().isBlank())
                ? "%" + filter.getKeyword().trim().toLowerCase() + "%"
                : null;
        String statusFilter = (filter.getStatus() != null && !filter.getStatus().isBlank()) ? filter.getStatus().trim().toUpperCase() : null;

        // 2. Fetch products using keyset cursor pagination with eager fetch joins
        List<Product> products = (filter.getCursor() == null)
                ? productRepository.findAdminInitial(statusFilter, filter.getCategoryId(), filter.getBrandId(),
                filter.getMinPrice(), filter.getMaxPrice(), keywordPattern, pageable)
                : productRepository.findAdminAfterCursor(statusFilter, filter.getCursor(), filter.getCategoryId(),
                filter.getBrandId(), filter.getMinPrice(), filter.getMaxPrice(), keywordPattern, pageable);

        // 3. Evaluate next cursor and truncate extra element
        boolean hasNext = products.size() > pageSize;
        List<Product> results = hasNext ? products.subList(0, pageSize) : products;
        String nextCursor = (hasNext && !results.isEmpty()) ? results.get(results.size() - 1).getId().toString() : null;

        // 4. Map entities to summary DTOs and populate price ranges
        List<ProductSummaryResponse> responseList = new ArrayList<>();
        for (Product product : results) {
            ProductSummaryResponse summary = productMapper.toSummaryResponse(product);
            summary.setSuppliers(product.getProductSuppliers() == null ? List.of() : product.getProductSuppliers().stream()
                    .map(ProductSupplier::getSupplier)
                    .filter(java.util.Objects::nonNull)
                    .map(supplierMapper::toResponse)
                    .toList());
            List<ProductVariant> variants = productVariantRepository.findByProductIdWithDetails(product.getId(), STATUS_DELETED);
            if (!variants.isEmpty()) {
                long minPrice = variants.stream().map(ProductVariant::getListPrice)
                        .filter(java.util.Objects::nonNull)
                        .mapToLong(Long::longValue).min().orElse(0L);
                long maxPrice = variants.stream().map(ProductVariant::getListPrice)
                        .filter(java.util.Objects::nonNull)
                        .mapToLong(Long::longValue).max().orElse(0L);
                summary.setMinPrice(minPrice);
                summary.setMaxPrice(maxPrice);
            }
            responseList.add(summary);
        }

        // 5. Return assembled cursor page response
        return CursorPageResponse.<ProductSummaryResponse>builder()
                .items(responseList)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .size(responseList.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getAdminProductById(UUID id) {
        Product product = productRepository.findAdminDetailById(id)
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_NOT_FOUND));

        List<ProductVariant> variants = productVariantRepository.findByProductIdWithDetails(id, STATUS_DELETED);
        ProductDetailResponse response = productMapper.toDetailResponse(product);
        response.setSuppliers(product.getProductSuppliers() == null ? List.of() : product.getProductSuppliers().stream()
                .map(ProductSupplier::getSupplier)
                .filter(java.util.Objects::nonNull)
                .map(supplierMapper::toResponse)
                .toList());
        response.setVariants(productVariantMapper.toResponseList(variants));
        return response;
    }

    @Override
    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request, UUID adminId) {
        // 1. Validate SEO name uniqueness
        if (productRepository.existsBySeoName(request.getSeoName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Product with SEO name '" + request.getSeoName() + "' already exists");
        }

        // 2. Verify foreign key relations (Category, Brand, Supplier)
        Category category = categoryRepository.findById(request.getCategoryId())
                .filter(c -> STATUS_ACTIVE.equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_NOT_FOUND));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .filter(b -> STATUS_ACTIVE.equalsIgnoreCase(b.getStatus()))
                    .orElseThrow(() -> new BusinessException(StatusCode.BRAND_NOT_FOUND));
        }

        List<Supplier> suppliers = resolveSuppliers(request.getSupplierIds());

        // 3. Save core Product entity
        UUID employeeId = resolveEmployeeId(adminId);
        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setBrand(brand);
        product.setCreatedBy(employeeId);
        Product savedProduct = productRepository.save(product);
        for (Supplier selectedSupplier : suppliers) {
            productSupplierRepository.save(ProductSupplier.builder()
                    .id(new ProductSupplier.ProductSupplierId(savedProduct.getId(), selectedSupplier.getId()))
                    .product(savedProduct)
                    .supplier(selectedSupplier)
                    .build());
        }

        // 4. Process initial variants if provided
        List<ProductVariant> savedVariants = new ArrayList<>();
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            for (CreateProductVariantRequest variantReq : request.getVariants()) {
                if (productVariantRepository.existsBySku(variantReq.getSku())) {
                    throw new BusinessException(StatusCode.SKU_ALREADY_EXISTS, "SKU '" + variantReq.getSku() + "' already exists");
                }
                if (variantReq.getBarcode() != null && !variantReq.getBarcode().isBlank()
                        && productVariantRepository.existsByBarcode(variantReq.getBarcode())) {
                    throw new BusinessException(StatusCode.CONFLICT, "Barcode '" + variantReq.getBarcode() + "' already exists");
                }

                ProductVariant variant = productVariantMapper.toEntity(variantReq);
                variant.setProduct(savedProduct);
                variant.setCreatedBy(employeeId);
                ProductVariant savedVariant = productVariantRepository.save(variant);

                // Link variant options
                if (variantReq.getOptionIds() != null && !variantReq.getOptionIds().isEmpty()) {
                    validateOptionTypes(variantReq.getOptionIds());
                    List<VariantOption> variantOptions = new ArrayList<>();
                    for (UUID optionId : variantReq.getOptionIds()) {
                        Option option = optionRepository.findById(optionId)
                                .filter(o -> STATUS_ACTIVE.equalsIgnoreCase(o.getStatus()))
                                .orElseThrow(() -> new BusinessException(StatusCode.OPTION_NOT_FOUND));
                        VariantOption vo = VariantOption.builder()
                                .productVariant(savedVariant)
                                .option(option)
                                .build();
                        variantOptions.add(variantOptionRepository.save(vo));
                    }
                    savedVariant.setVariantOptions(new java.util.LinkedHashSet<>(variantOptions));
                }

                // Save variant images
                if (variantReq.getImages() != null && !variantReq.getImages().isEmpty()) {
                    validateMainImages(variantReq.getImages());
                    List<ProductImage> productImages = new ArrayList<>();
                    for (CreateProductImageRequest imgReq : variantReq.getImages()) {
                        demoteExistingMainImage(savedVariant, imgReq);
                        ProductImage img = productImageMapper.toEntity(imgReq);
                        img.setProductVariant(savedVariant);
                        img.setFile(resolveFile(imgReq));
                        productImages.add(productImageRepository.save(img));
                    }
                    savedVariant.setImages(new java.util.LinkedHashSet<>(productImages));
                }

                savedVariants.add(savedVariant);
            }
        }

        // 5. Assemble and return full ProductDetailResponse DTO
        ProductDetailResponse response = productMapper.toDetailResponse(savedProduct);
        response.setVariants(productVariantMapper.toResponseList(savedVariants));
        response.setSuppliers(supplierMapper.toResponseList(suppliers));
        return response;
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(UUID id, UpdateProductRequest request, UUID adminId) {
        // 1. Fetch existing product
        Product product = productRepository.findById(id)
                .filter(p -> !STATUS_DELETED.equalsIgnoreCase(p.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_NOT_FOUND));

        // Soft deletion has its own guarded endpoint.  Do not let a generic
        // edit bypass the historical-order safety check.
        validateMutableStatus(request.getStatus());

        // 2. Validate SEO name uniqueness if modified
        if (!product.getSeoName().equalsIgnoreCase(request.getSeoName()) && productRepository.existsBySeoName(request.getSeoName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Product with SEO name '" + request.getSeoName() + "' already exists");
        }

        // 3. Verify category, brand, supplier references
        Category category = categoryRepository.findById(request.getCategoryId())
                .filter(c -> STATUS_ACTIVE.equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_NOT_FOUND));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .filter(b -> STATUS_ACTIVE.equalsIgnoreCase(b.getStatus()))
                    .orElseThrow(() -> new BusinessException(StatusCode.BRAND_NOT_FOUND));
        }

        List<Supplier> suppliers = resolveSuppliers(request.getSupplierIds());

        // 4. Update entity fields via MapStruct @MappingTarget
        UUID employeeId = resolveEmployeeId(adminId);
        productMapper.updateEntityFromRequest(request, product);
        product.setCategory(category);
        product.setBrand(brand);
        productSupplierRepository.deleteByProductId(id);
        for (Supplier selectedSupplier : suppliers) {
            productSupplierRepository.save(ProductSupplier.builder()
                    .id(new ProductSupplier.ProductSupplierId(id, selectedSupplier.getId()))
                    .product(product)
                    .supplier(selectedSupplier)
                    .build());
        }
        product.setUpdatedBy(employeeId);
        Product updatedProduct = productRepository.save(product);

        // 5. Fetch variants and assemble response DTO
        List<ProductVariant> variants = productVariantRepository.findByProductIdWithDetails(id, STATUS_DELETED);
        ProductDetailResponse response = productMapper.toDetailResponse(updatedProduct);
        response.setVariants(productVariantMapper.toResponseList(variants));
        response.setSuppliers(supplierMapper.toResponseList(suppliers));
        return response;
    }

    @Override
    @Transactional
    public void updateProductStatus(UUID id, String status, UUID adminId) {
        // 1. Retrieve product entity
        Product product = productRepository.findById(id)
                .filter(p -> !STATUS_DELETED.equalsIgnoreCase(p.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_NOT_FOUND));

        // 2. Hide/restore only.  DELETED must go through deleteProduct(),
        // which checks whether the product has existing order history.
        validateMutableStatus(status);

        // 3. Update status and audit metadata
        UUID employeeId = resolveEmployeeId(adminId);
        product.setStatus(status.toUpperCase());
        product.setUpdatedBy(employeeId);
        productRepository.save(product);
        log.info("Updated product [{}] status to [{}] by employee [{}]", id, status, employeeId);
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

    private void validatePriceRange(ProductFilterRequest filter) {
        Long minPrice = filter.getMinPrice();
        Long maxPrice = filter.getMaxPrice();
        if ((minPrice != null && minPrice < 0)
                || (maxPrice != null && maxPrice < 0)
                || (minPrice != null && maxPrice != null && minPrice > maxPrice)) {
            throw new BusinessException(StatusCode.VALIDATION_ERROR,
                    "Minimum price must be less than or equal to maximum price");
        }
    }

    private void validateMutableStatus(String status) {
        if (status != null && !"ACTIVE".equalsIgnoreCase(status)
                && !"INACTIVE".equalsIgnoreCase(status)) {
            throw new BusinessException(StatusCode.BAD_REQUEST,
                    "Only ACTIVE or INACTIVE is allowed here; use the delete endpoint for DELETED");
        }
    }

    private List<Supplier> resolveSuppliers(List<UUID> requestedIds) {
        Set<UUID> ids = new java.util.LinkedHashSet<>();
        if (requestedIds != null) {
            requestedIds.stream().filter(java.util.Objects::nonNull).forEach(ids::add);
        }
        List<Supplier> suppliers = new ArrayList<>();
        for (UUID supplierId : ids) {
            suppliers.add(supplierRepository.findById(supplierId)
                    .filter(s -> STATUS_ACTIVE.equalsIgnoreCase(s.getStatus()))
                    .orElseThrow(() -> new BusinessException(StatusCode.SUPPLIER_NOT_FOUND)));
        }
        return suppliers;
    }

    private File resolveFile(CreateProductImageRequest request) {
        return fileRepository.findById(request.getFileId())
                .filter(file -> "ACTIVE".equalsIgnoreCase(file.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.IMAGE_NOT_FOUND, "Referenced file was not found"));
    }

    private void validateOptionTypes(List<UUID> optionIds) {
        Set<String> types = new HashSet<>();
        for (UUID optionId : optionIds) {
            Option option = optionRepository.findById(optionId)
                    .filter(o -> STATUS_ACTIVE.equalsIgnoreCase(o.getStatus()))
                    .orElseThrow(() -> new BusinessException(StatusCode.OPTION_NOT_FOUND));
            if (!types.add(option.getType().trim().toUpperCase())) {
                throw new BusinessException(StatusCode.BAD_REQUEST,
                        "A variant cannot contain more than one option of the same type");
            }
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

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        // 1. Retrieve product entity
        Product product = productRepository.findById(id)
                .filter(p -> !STATUS_DELETED.equalsIgnoreCase(p.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_NOT_FOUND));

        // 2. Enforce the inventory/history safety boundary.  The MVP keeps
        // only the current aggregate stock on each variant (there is no stock
        // movement ledger), so a product with any remaining quantity is
        // treated as already imported and cannot be deleted.  Order history is
        // checked separately because those rows must remain addressable.
        List<ProductVariant> variants = productVariantRepository.findByProductIdWithDetails(id, STATUS_DELETED);
        if (variants != null && variants.stream()
                .filter(variant -> !STATUS_DELETED.equalsIgnoreCase(variant.getStatus()))
                .anyMatch(variant -> variant.getQuantity() != null && variant.getQuantity() > 0)) {
            throw new BusinessException(StatusCode.CONFLICT,
                    "Cannot delete product with remaining inventory stock");
        }

        // 3. Enforce safety constraint: verify product hasn't been ordered
        long orderCount = orderItemRepository.countByProductVariantProductId(id);
        if (orderCount > 0) {
            throw new BusinessException(StatusCode.CONFLICT, "Cannot delete product with existing customer orders");
        }

        // 4. Perform soft delete on product and associated variants
        product.setStatus(STATUS_DELETED);
        productRepository.save(product);

        for (ProductVariant variant : variants == null ? List.<ProductVariant>of() : variants) {
            variant.setStatus(STATUS_DELETED);
            productVariantRepository.save(variant);
        }
        log.info("Soft deleted product with id: {}", id);
    }
}
