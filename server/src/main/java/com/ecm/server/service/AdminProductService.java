package com.ecm.server.service;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.CreateProductRequest;
import com.ecm.server.dto.request.ProductFilterRequest;
import com.ecm.server.dto.request.UpdateProductRequest;
import com.ecm.server.dto.response.ProductDetailResponse;
import com.ecm.server.dto.response.ProductSummaryResponse;

import java.util.UUID;

public interface AdminProductService {

    CursorPageResponse<ProductSummaryResponse> getAdminProducts(ProductFilterRequest filter);

    ProductDetailResponse createProduct(CreateProductRequest request, UUID adminId);

    ProductDetailResponse updateProduct(UUID id, UpdateProductRequest request, UUID adminId);

    void updateProductStatus(UUID id, String status, UUID adminId);

    void deleteProduct(UUID id);
}
