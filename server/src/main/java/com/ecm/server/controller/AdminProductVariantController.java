package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.config.security.UserPrincipal;
import com.ecm.server.dto.request.CreateProductVariantRequest;
import com.ecm.server.dto.request.UpdateProductVariantRequest;
import com.ecm.server.dto.response.ProductVariantResponse;
import com.ecm.server.service.AdminProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
public class AdminProductVariantController {

    private final AdminProductVariantService adminProductVariantService;

    @PostMapping("/products/{productId}/variants")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(
            @PathVariable UUID productId,
            @Valid @RequestBody CreateProductVariantRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID adminId = (principal != null) ? principal.getAccountId() : null;
        ProductVariantResponse response = adminProductVariantService.createVariant(productId, request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(StatusCode.CREATED, response));
    }

    @PutMapping("/variants/{variantId}")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateProductVariantRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID adminId = (principal != null) ? principal.getAccountId() : null;
        ProductVariantResponse response = adminProductVariantService.updateVariant(variantId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.UPDATED, response));
    }

    @DeleteMapping("/variants/{variantId}")
    public ResponseEntity<ApiResponse<String>> deleteVariant(@PathVariable UUID variantId) {
        adminProductVariantService.deleteVariant(variantId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Product variant deleted successfully."));
    }
}
