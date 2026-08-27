package com.ecm.server.service;

import com.ecm.server.dto.request.CreateProductImageRequest;
import com.ecm.server.dto.request.CreateProductVariantRequest;
import com.ecm.server.dto.request.UpdateProductVariantRequest;
import com.ecm.server.dto.response.ProductImageResponse;
import com.ecm.server.dto.response.ProductVariantResponse;

import java.util.UUID;

public interface AdminProductVariantService {

    ProductVariantResponse createVariant(UUID productId, CreateProductVariantRequest request, UUID adminId);

    ProductVariantResponse updateVariant(UUID variantId, UpdateProductVariantRequest request, UUID adminId);

    void deleteVariant(UUID variantId);

    ProductImageResponse addVariantImage(UUID variantId, CreateProductImageRequest request);

    void deleteImage(UUID imageId);
}
