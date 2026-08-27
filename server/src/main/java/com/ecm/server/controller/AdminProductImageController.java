package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateProductImageRequest;
import com.ecm.server.dto.response.ProductImageResponse;
import com.ecm.server.service.AdminProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
public class AdminProductImageController {

    private final AdminProductVariantService adminProductVariantService;

    @PostMapping("/variants/{variantId}/images")
    public ResponseEntity<ApiResponse<ProductImageResponse>> addVariantImage(
            @PathVariable UUID variantId,
            @Valid @RequestBody CreateProductImageRequest request
    ) {
        ProductImageResponse response = adminProductVariantService.addVariantImage(variantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(StatusCode.CREATED, response));
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<ApiResponse<String>> deleteImage(@PathVariable UUID imageId) {
        adminProductVariantService.deleteImage(imageId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Product image deleted successfully."));
    }
}
