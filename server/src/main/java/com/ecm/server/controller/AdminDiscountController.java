package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.config.security.UserPrincipal;
import com.ecm.server.dto.request.CreateDiscountRequest;
import com.ecm.server.dto.request.DiscountFilterRequest;
import com.ecm.server.dto.request.UpdateDiscountRequest;
import com.ecm.server.dto.response.DiscountDetailResponse;
import com.ecm.server.dto.response.DiscountSummaryResponse;
import com.ecm.server.service.AdminDiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/discounts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE', 'ROLE_MANAGER')")
public class AdminDiscountController {

    private final AdminDiscountService adminDiscountService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<DiscountSummaryResponse>>> getAdminDiscounts(
            @ModelAttribute DiscountFilterRequest filter
    ) {
        CursorPageResponse<DiscountSummaryResponse> response = adminDiscountService.getAdminDiscounts(filter);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DiscountDetailResponse>> getDiscountById(@PathVariable UUID id) {
        DiscountDetailResponse response = adminDiscountService.getDiscountById(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DiscountDetailResponse>> createDiscount(
            @Valid @RequestBody CreateDiscountRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID adminId = (principal != null) ? principal.getAccountId() : null;
        DiscountDetailResponse response = adminDiscountService.createDiscount(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(StatusCode.CREATED, response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DiscountDetailResponse>> updateDiscount(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDiscountRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID adminId = (principal != null) ? principal.getAccountId() : null;
        DiscountDetailResponse response = adminDiscountService.updateDiscount(id, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.UPDATED, response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateDiscountStatus(
            @PathVariable UUID id,
            @Valid @RequestBody com.ecm.server.dto.request.UpdateStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID adminId = (principal != null) ? principal.getAccountId() : null;
        adminDiscountService.updateDiscountStatus(id, request.getStatus(), adminId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Discount status updated successfully."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteDiscount(@PathVariable UUID id) {
        adminDiscountService.deleteDiscount(id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, "Discount deleted successfully."));
    }
}
