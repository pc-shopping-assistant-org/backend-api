package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.AssignAttributeRequest;
import com.ecm.server.dto.request.CreateCategoryGroupRequest;
import com.ecm.server.dto.request.UpdateCategoryGroupRequest;
import com.ecm.server.dto.response.CategoryAttributeGroupResponse;
import com.ecm.server.dto.response.CategoryAttributeResponse;
import com.ecm.server.dto.response.CategorySpecsSchemaResponse;
import com.ecm.server.service.CategoryAttributeGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CategoryAttributeGroupController {

    private final CategoryAttributeGroupService categoryAttributeGroupService;

    // Public Endpoint
    @GetMapping("/api/v1/categories/{categoryId}/specs-schema")
    public ResponseEntity<ApiResponse<CategorySpecsSchemaResponse>> getCategorySpecsSchema(@PathVariable UUID categoryId) {
        CategorySpecsSchemaResponse response = categoryAttributeGroupService.getCategorySpecsSchema(categoryId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    // Admin Endpoints
    @PostMapping("/api/v1/admin/category-attributes/groups")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<CategoryAttributeGroupResponse>> createGroup(
            @Valid @RequestBody CreateCategoryGroupRequest request
    ) {
        CategoryAttributeGroupResponse response = categoryAttributeGroupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(StatusCode.CREATED, response));
    }

    @PutMapping("/api/v1/admin/category-attributes/groups/{groupId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<CategoryAttributeGroupResponse>> updateGroup(
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateCategoryGroupRequest request
    ) {
        CategoryAttributeGroupResponse response = categoryAttributeGroupService.updateGroup(groupId, request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.UPDATED, response));
    }

    @PostMapping("/api/v1/admin/category-attributes/assign")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<CategoryAttributeResponse>> assignAttribute(
            @Valid @RequestBody AssignAttributeRequest request
    ) {
        CategoryAttributeResponse response = categoryAttributeGroupService.assignAttribute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(StatusCode.CREATED, response));
    }

    @DeleteMapping("/api/v1/admin/category-attributes/assign/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<String>> unassignAttribute(@PathVariable UUID id) {
        categoryAttributeGroupService.unassignAttribute(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Attribute unassigned successfully."));
    }
}
