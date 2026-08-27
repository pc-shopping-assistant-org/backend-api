package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.ProductFilterRequest;
import com.ecm.server.dto.response.ProductDetailResponse;
import com.ecm.server.dto.response.ProductSummaryResponse;
import com.ecm.server.dto.response.ProductVariantResponse;
import com.ecm.server.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<ProductSummaryResponse>>> getProducts(
            @ModelAttribute ProductFilterRequest filter
    ) {
        CursorPageResponse<ProductSummaryResponse> response = productService.getProducts(filter);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(@PathVariable UUID id) {
        ProductDetailResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/slug/{seoName}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductBySlug(@PathVariable String seoName) {
        ProductDetailResponse response = productService.getProductBySlug(seoName);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/{id}/variants/{variantId}")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getProductVariant(
            @PathVariable UUID id,
            @PathVariable UUID variantId
    ) {
        ProductVariantResponse response = productService.getProductVariant(id, variantId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }
}
