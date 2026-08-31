package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.response.PaymentMethodResponse;
import com.ecm.server.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping("/api/v1/payment-methods")
    public ResponseEntity<ApiResponse<List<PaymentMethodResponse>>> getPaymentMethods() {
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS,
                paymentMethodService.getActivePaymentMethods()));
    }

    @GetMapping("/api/v1/admin/payment-methods")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<List<PaymentMethodResponse>>> getAdminPaymentMethods() {
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS,
                paymentMethodService.getActivePaymentMethods()));
    }
}
