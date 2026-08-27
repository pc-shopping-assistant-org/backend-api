package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CustomerFilterRequest;
import com.ecm.server.dto.request.UpdateUserStatusRequest;
import com.ecm.server.dto.response.CustomerDetailResponse;
import com.ecm.server.dto.response.CustomerOrderSummaryResponse;
import com.ecm.server.service.AdminCustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<CustomerDetailResponse>>> getCustomers(
            @ModelAttribute CustomerFilterRequest request
    ) {
        CursorPageResponse<CustomerDetailResponse> response = adminCustomerService.getCustomers(request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> getCustomerById(@PathVariable UUID id) {
        CustomerDetailResponse response = adminCustomerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<ApiResponse<List<CustomerOrderSummaryResponse>>> getCustomerOrders(@PathVariable UUID id) {
        List<CustomerOrderSummaryResponse> response = adminCustomerService.getCustomerOrders(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateCustomerStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        adminCustomerService.updateCustomerStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Customer status updated successfully."));
    }
}
