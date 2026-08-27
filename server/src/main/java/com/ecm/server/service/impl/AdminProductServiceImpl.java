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
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    public static final String STATUS_DELETED = "DELETED";
    public static final int DEFAULT_LIMIT = 20;

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final OptionRepository optionRepository;
    private final VariantOptionRepository variantOptionRepository;
    private final OrderItemRepository orderItemRepository;
    private final com.ecm.server.repository.EmployeeRepository employeeRepository;
    private final ProductMapper productMapper;
    private final ProductVariantMapper productVariantMapper;
    private final ProductImageMapper productImageMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<ProductSummaryResponse> getAdminProducts(ProductFilterRequest filter) {
        // 1. Prepare pagination pageable and search parameters
        int pageSize = (filter.getLimit() != null && filter.getLimit() > 0) ? filter.getLimit() : DEFAULT_LIMIT;
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        String keywordPattern = (filter.getKeyword() != null && !filter.getKeyword().isBlank())
                ? "%" + filter.getKeyword().trim().toLowerCase() + "%"
                : null;
        String statusFilter = (filter.getStatus() != null && !filter.getStatus().isBlank()) ? filter.getStatus().trim().toUpperCase() : null;

        // 2. Fetch products using keyset cursor pagination with eager fetch joins
        List<Product> products = (filter.getCursor() == null)
                ? productRepository.findAdminInitial(statusFilter, filter.getCategoryId(), filter.getBrandId(), keywordPattern, pageable)
                : productRepository.findAdminAfterCursor(statusFilter, filter.getCursor(), filter.getCategoryId(), filter.getBrandId(), keywordPattern, pageable);

        // 3. Evaluate next cursor and truncate extra element
        boolean hasNext = products.size() > pageSize;
        List<Product> results = hasNext ? products.subList(0, pageSize) : products;
        String nextCursor = (hasNext && !results.isEmpty()) ? results.get(results.size() - 1).getId().toString() : null;

        // 4. Map entities to summary DTOs and populate price ranges
        List<ProductSummaryResponse> responseList = new ArrayList<>();
        for (Product product : results) {
            ProductSummaryResponse summary = productMapper.toSummaryResponse(product);
            List<ProductVariant> variants = productVariantRepository.findByProductIdWithDetails(product.getId(), STATUS_DELETED);
            if (!variants.isEmpty()) {
                int minPrice = variants.stream().mapToInt(ProductVariant::getPriceSale).min().orElse(0);
                int maxPrice = variants.stream().mapToInt(ProductVariant::getPrice).max().orElse(0);
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
    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request, UUID adminId) {
        // 1. Validate SEO name uniqueness
        if (productRepository.existsBySeoName(request.getSeoName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Product with SEO name '" + request.getSeoName() + "' already exists");
        }

        // 2. Verify foreign key relations (Category, Brand, Supplier)
        Category category = categoryRepository.findById(request.getCategoryId())
                .filter(c -> !STATUS_DELETED.equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_NOT_FOUND));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .filter(b -> !STATUS_DELETED.equalsIgnoreCase(b.getStatus()))
                    .orElseThrow(() -> new BusinessException(StatusCode.BRAND_NOT_FOUND));
        }

        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = supplierRepository.findById(request.getSupplierId())
                    .filter(s -> !STATUS_DELETED.equalsIgnoreCase(s.getStatus()))
                    .orElseThrow(() -> new BusinessException(StatusCode.SUPPLIER_NOT_FOUND));
        }

        // 3. Save core Product entity
        UUID employeeId = resolveEmployeeId(adminId);
        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setBrand(brand);
        product.setSupplier(supplier);
        product.setCreatedBy(employeeId);
        Product savedProduct = productRepository.save(product);

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
                    List<VariantOption> variantOptions = new ArrayList<>();
                    for (UUID optionId : variantReq.getOptionIds()) {
                        Option option = optionRepository.findById(optionId)
                                .filter(o -> !STATUS_DELETED.equalsIgnoreCase(o.getStatus()))
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
                    List<ProductImage> productImages = new ArrayList<>();
                    for (CreateProductImageRequest imgReq : variantReq.getImages()) {
                        ProductImage img = productImageMapper.toEntity(imgReq);
                        img.setProductVariant(savedVariant);
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
        return response;
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(UUID id, UpdateProductRequest request, UUID adminId) {
        // 1. Fetch existing product
        Product product = productRepository.findById(id)
                .filter(p -> !STATUS_DELETED.equalsIgnoreCase(p.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_NOT_FOUND));

        // 2. Validate SEO name uniqueness if modified
        if (!product.getSeoName().equalsIgnoreCase(request.getSeoName()) && productRepository.existsBySeoName(request.getSeoName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Product with SEO name '" + request.getSeoName() + "' already exists");
        }

        // 3. Verify category, brand, supplier references
        Category category = categoryRepository.findById(request.getCategoryId())
                .filter(c -> !STATUS_DELETED.equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_NOT_FOUND));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .filter(b -> !STATUS_DELETED.equalsIgnoreCase(b.getStatus()))
                    .orElseThrow(() -> new BusinessException(StatusCode.BRAND_NOT_FOUND));
        }

        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = supplierRepository.findById(request.getSupplierId())
                    .filter(s -> !STATUS_DELETED.equalsIgnoreCase(s.getStatus()))
                    .orElseThrow(() -> new BusinessException(StatusCode.SUPPLIER_NOT_FOUND));
        }

        // 4. Update entity fields via MapStruct @MappingTarget
        UUID employeeId = resolveEmployeeId(adminId);
        productMapper.updateEntityFromRequest(request, product);
        product.setCategory(category);
        product.setBrand(brand);
        product.setSupplier(supplier);
        product.setUpdatedBy(employeeId);
        Product updatedProduct = productRepository.save(product);

        // 5. Fetch variants and assemble response DTO
        List<ProductVariant> variants = productVariantRepository.findByProductIdWithDetails(id, STATUS_DELETED);
        ProductDetailResponse response = productMapper.toDetailResponse(updatedProduct);
        response.setVariants(productVariantMapper.toResponseList(variants));
        return response;
    }

    @Override
    @Transactional
    public void updateProductStatus(UUID id, String status, UUID adminId) {
        // 1. Retrieve product entity
        Product product = productRepository.findById(id)
                .filter(p -> !STATUS_DELETED.equalsIgnoreCase(p.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_NOT_FOUND));

        // 2. Update status and audit metadata
        UUID employeeId = resolveEmployeeId(adminId);
        product.setStatus(status.toUpperCase());
        product.setUpdatedBy(employeeId);
        productRepository.save(product);
        log.info("Updated product [{}] status to [{}] by employee [{}]", id, status, employeeId);
    }

    private UUID resolveEmployeeId(UUID accountId) {
        if (accountId == null) {
            return employeeRepository.findAll().stream().findFirst().map(com.ecm.server.model.Employee::getId).orElse(null);
        }
        return employeeRepository.findByAccountId(accountId)
                .map(com.ecm.server.model.Employee::getId)
                .or(() -> employeeRepository.findById(accountId).map(com.ecm.server.model.Employee::getId))
                .orElseGet(() -> employeeRepository.findAll().stream().findFirst().map(com.ecm.server.model.Employee::getId).orElse(null));
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        // 1. Retrieve product entity
        Product product = productRepository.findById(id)
                .filter(p -> !STATUS_DELETED.equalsIgnoreCase(p.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_NOT_FOUND));

        // 2. Enforce safety constraint: verify product hasn't been ordered
        long orderCount = orderItemRepository.countByProductVariantProductId(id);
        if (orderCount > 0) {
            throw new BusinessException(StatusCode.CONFLICT, "Cannot delete product with existing customer orders");
        }

        // 3. Perform soft delete on product and associated variants
        product.setStatus(STATUS_DELETED);
        productRepository.save(product);

        List<ProductVariant> variants = productVariantRepository.findByProductIdWithDetails(id, STATUS_DELETED);
        for (ProductVariant variant : variants) {
            variant.setStatus(STATUS_DELETED);
            productVariantRepository.save(variant);
        }
        log.info("Soft deleted product with id: {}", id);
    }
}
