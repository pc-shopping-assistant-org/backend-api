package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.config.security.UserPrincipal;
import com.ecm.server.dto.request.CreatePaymentIntentRequest;
import com.ecm.server.dto.response.PaymentDetailResponse;
import com.ecm.server.dto.response.PaymentIntentResponse;
import com.ecm.server.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-intent")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> createPaymentIntent(
            @Valid @RequestBody CreatePaymentIntentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PaymentIntentResponse response = paymentService.createPaymentIntent(principal.getAccountId(), request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getPaymentByOrderId(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PaymentDetailResponse response = paymentService.getPaymentByOrderId(principal.getAccountId(), orderId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PostMapping("/{paymentId}/confirm-cod")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> confirmCodPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PaymentDetailResponse response = paymentService.confirmCodPayment(principal.getAccountId(), paymentId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }
}
