package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateEmployeeRequest;
import com.ecm.server.dto.request.EmployeeFilterRequest;
import com.ecm.server.dto.request.UpdateEmployeeRequest;
import com.ecm.server.dto.request.UpdateUserStatusRequest;
import com.ecm.server.dto.response.EmployeeDetailResponse;
import com.ecm.server.service.AdminEmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminEmployeeController {

    private final AdminEmployeeService adminEmployeeService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<EmployeeDetailResponse>>> getEmployees(
            @ModelAttribute EmployeeFilterRequest request
    ) {
        CursorPageResponse<EmployeeDetailResponse> response = adminEmployeeService.getEmployees(request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDetailResponse>> getEmployeeById(@PathVariable UUID id) {
        EmployeeDetailResponse response = adminEmployeeService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeDetailResponse>> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request
    ) {
        EmployeeDetailResponse response = adminEmployeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(StatusCode.CREATED, response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDetailResponse>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        EmployeeDetailResponse response = adminEmployeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.UPDATED, response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateEmployeeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        adminEmployeeService.updateEmployeeStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Employee status updated successfully."));
    }
}
