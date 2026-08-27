package com.ecm.server.service;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.ProductFilterRequest;
import com.ecm.server.dto.response.ProductDetailResponse;
import com.ecm.server.dto.response.ProductSummaryResponse;
import com.ecm.server.dto.response.ProductVariantResponse;

import java.util.UUID;

public interface ProductService {

    CursorPageResponse<ProductSummaryResponse> getProducts(ProductFilterRequest filter);

    ProductDetailResponse getProductById(UUID id);

    ProductDetailResponse getProductBySlug(String seoName);

    ProductVariantResponse getProductVariant(UUID productId, UUID variantId);
}
