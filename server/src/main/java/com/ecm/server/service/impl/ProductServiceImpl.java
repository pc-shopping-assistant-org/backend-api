package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.ProductFilterRequest;
import com.ecm.server.dto.response.ProductDetailResponse;
import com.ecm.server.dto.response.ProductSummaryResponse;
import com.ecm.server.dto.response.ProductVariantResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.ProductMapper;
import com.ecm.server.mapper.ProductVariantMapper;
import com.ecm.server.model.Product;
import com.ecm.server.model.ProductVariant;
import com.ecm.server.repository.ProductRepository;
import com.ecm.server.repository.ProductVariantRepository;
import com.ecm.server.service.ProductService;
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
public class ProductServiceImpl implements ProductService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DELETED = "DELETED";
    public static final int DEFAULT_LIMIT = 20;

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductMapper productMapper;
    private final ProductVariantMapper productVariantMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<ProductSummaryResponse> getProducts(ProductFilterRequest filter) {
        // 1. Sanitize pagination limit and search keyword pattern
        int pageSize = (filter.getLimit() != null && filter.getLimit() > 0) ? filter.getLimit() : DEFAULT_LIMIT;
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        String keywordPattern = (filter.getKeyword() != null && !filter.getKeyword().isBlank())
                ? "%" + filter.getKeyword().trim().toLowerCase() + "%"
                : null;

        // 2. Fetch products using keyset cursor pagination with eager fetch joins
        List<Product> products = (filter.getCursor() == null)
                ? productRepository.findInitial(STATUS_ACTIVE, filter.getCategoryId(), filter.getBrandId(), keywordPattern, pageable)
                : productRepository.findAfterCursor(STATUS_ACTIVE, filter.getCursor(), filter.getCategoryId(), filter.getBrandId(), keywordPattern, pageable);

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

        // 5. Assemble and return cursor page response
        return CursorPageResponse.<ProductSummaryResponse>builder()
                .items(responseList)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .size(responseList.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(UUID id) {
        // 1. Fetch product by ID with eager relationships
        Product product = productRepository.findDetailById(id, STATUS_DELETED)
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_NOT_FOUND));

        // 2. Fetch all active product variants with details
        List<ProductVariant> variants = productVariantRepository.findByProductIdWithDetails(id, STATUS_DELETED);

        // 3. Map to response DTO and assemble variant hierarchy
        ProductDetailResponse response = productMapper.toDetailResponse(product);
        response.setVariants(productVariantMapper.toResponseList(variants));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductBySlug(String seoName) {
        // 1. Fetch product by SEO slug with eager relationships
        Product product = productRepository.findDetailBySeoName(seoName, STATUS_DELETED)
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_NOT_FOUND));

        // 2. Fetch all active product variants with details
        List<ProductVariant> variants = productVariantRepository.findByProductIdWithDetails(product.getId(), STATUS_DELETED);

        // 3. Map to response DTO and assemble variant hierarchy
        ProductDetailResponse response = productMapper.toDetailResponse(product);
        response.setVariants(productVariantMapper.toResponseList(variants));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getProductVariant(UUID productId, UUID variantId) {
        // 1. Fetch specific variant by ID and product ID
        ProductVariant variant = productVariantRepository.findByIdAndProductIdWithDetails(variantId, productId, STATUS_DELETED)
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_VARIANT_NOT_FOUND));

        // 2. Map and return variant response DTO
        return productVariantMapper.toResponse(variant);
    }
}
