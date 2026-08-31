package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.config.security.UserPrincipal;
import com.ecm.server.dto.request.CreateProductRequest;
import com.ecm.server.dto.request.ProductFilterRequest;
import com.ecm.server.dto.request.UpdateProductRequest;
import com.ecm.server.dto.request.UpdateProductStatusRequest;
import com.ecm.server.dto.response.ProductDetailResponse;
import com.ecm.server.dto.response.ProductSummaryResponse;
import com.ecm.server.service.AdminProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<ProductSummaryResponse>>> getAdminProducts(
            @Valid @ModelAttribute ProductFilterRequest filter
    ) {
        CursorPageResponse<ProductSummaryResponse> response = adminProductService.getAdminProducts(filter);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID adminId = (principal != null) ? principal.getAccountId() : null;
        ProductDetailResponse response = adminProductService.createProduct(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(StatusCode.CREATED, response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID adminId = (principal != null) ? principal.getAccountId() : null;
        ProductDetailResponse response = adminProductService.updateProduct(id, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.UPDATED, response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateProductStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID adminId = (principal != null) ? principal.getAccountId() : null;
        adminProductService.updateProductStatus(id, request.getStatus(), adminId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Product status updated successfully."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable UUID id) {
        adminProductService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Product deleted successfully."));
    }
}
