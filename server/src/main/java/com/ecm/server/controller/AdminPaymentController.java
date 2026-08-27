package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.config.security.UserPrincipal;
import com.ecm.server.dto.request.PaymentFilterRequest;
import com.ecm.server.dto.request.UpdatePaymentStatusRequest;
import com.ecm.server.dto.response.PaymentDetailResponse;
import com.ecm.server.service.AdminPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE', 'ROLE_MANAGER')")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<PaymentDetailResponse>>> getAdminPayments(
            @ModelAttribute PaymentFilterRequest filter
    ) {
        CursorPageResponse<PaymentDetailResponse> response = adminPaymentService.getAdminPayments(filter);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> updatePaymentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePaymentStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID adminId = (principal != null) ? principal.getAccountId() : null;
        PaymentDetailResponse response = adminPaymentService.updatePaymentStatus(id, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }
}
