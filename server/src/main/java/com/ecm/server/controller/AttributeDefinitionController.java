package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateAttributeDefinitionRequest;
import com.ecm.server.dto.request.UpdateAttributeDefinitionRequest;
import com.ecm.server.dto.response.AttributeDefinitionResponse;
import com.ecm.server.service.AttributeDefinitionService;
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
public class AttributeDefinitionController {

    private final AttributeDefinitionService attributeDefinitionService;

    // Public Endpoints
    @GetMapping("/api/v1/attributes")
    public ResponseEntity<ApiResponse<List<AttributeDefinitionResponse>>> getAllAttributes() {
        List<AttributeDefinitionResponse> response = attributeDefinitionService.getAllAttributes();
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/api/v1/attributes/{id}")
    public ResponseEntity<ApiResponse<AttributeDefinitionResponse>> getAttributeById(@PathVariable UUID id) {
        AttributeDefinitionResponse response = attributeDefinitionService.getAttributeById(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    // Admin Endpoints
    @PostMapping("/api/v1/admin/attributes")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<AttributeDefinitionResponse>> createAttribute(
            @Valid @RequestBody CreateAttributeDefinitionRequest request
    ) {
        AttributeDefinitionResponse response = attributeDefinitionService.createAttribute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(StatusCode.CREATED, response));
    }

    @PutMapping("/api/v1/admin/attributes/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<AttributeDefinitionResponse>> updateAttribute(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAttributeDefinitionRequest request
    ) {
        AttributeDefinitionResponse response = attributeDefinitionService.updateAttribute(id, request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.UPDATED, response));
    }

    @DeleteMapping("/api/v1/admin/attributes/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<String>> deleteAttribute(@PathVariable UUID id) {
        attributeDefinitionService.deleteAttribute(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Attribute deleted successfully."));
    }
}
