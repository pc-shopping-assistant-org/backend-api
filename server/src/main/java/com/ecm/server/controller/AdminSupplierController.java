package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateSupplierRequest;
import com.ecm.server.dto.request.SupplierFilterRequest;
import com.ecm.server.dto.request.UpdateSupplierRequest;
import com.ecm.server.dto.response.SupplierResponse;
import com.ecm.server.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
public class AdminSupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<SupplierResponse>>> getSuppliers(
            @ModelAttribute SupplierFilterRequest request
    ) {
        CursorPageResponse<SupplierResponse> response = supplierService.getSuppliers(request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponse>> getSupplierById(@PathVariable UUID id) {
        SupplierResponse response = supplierService.getSupplierById(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
        SupplierResponse response = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(StatusCode.CREATED, response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplier(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupplierRequest request
    ) {
        SupplierResponse response = supplierService.updateSupplier(id, request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.UPDATED, response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteSupplier(@PathVariable UUID id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Supplier deleted successfully."));
    }
}
