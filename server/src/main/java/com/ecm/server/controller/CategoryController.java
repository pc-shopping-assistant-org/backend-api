package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateCategoryRequest;
import com.ecm.server.dto.request.UpdateCategoryRequest;
import com.ecm.server.dto.response.CategoryResponse;
import com.ecm.server.dto.response.CategoryTreeResponse;
import com.ecm.server.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // Public Endpoints
    @GetMapping("/api/v1/categories")
    public ResponseEntity<ApiResponse<List<CategoryTreeResponse>>> getCategoryTree() {
        List<CategoryTreeResponse> response = categoryService.getCategoryTree();
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/api/v1/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable UUID id) {
        CategoryResponse response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/api/v1/categories/slug/{seoName}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryBySlug(@PathVariable String seoName) {
        CategoryResponse response = categoryService.getCategoryBySlug(seoName);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    // Admin Endpoints
    @PostMapping("/api/v1/admin/categories")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(StatusCode.CREATED, response));
    }

    @PutMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.UPDATED, response));
    }

    @DeleteMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Category deleted successfully."));
    }
}
