package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.config.security.UserPrincipal;
import com.ecm.server.dto.request.CancelOrderRequest;
import com.ecm.server.dto.request.CreateOrderRequest;
import com.ecm.server.dto.request.OrderFilterRequest;
import com.ecm.server.dto.response.OrderDetailResponse;
import com.ecm.server.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDetailResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrderDetailResponse response = orderService.createOrder(principal.getAccountId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(StatusCode.CREATED, response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CursorPageResponse<OrderDetailResponse>>> getMyOrders(
            @ModelAttribute OrderFilterRequest filter,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CursorPageResponse<OrderDetailResponse> response = orderService.getMyOrders(principal.getAccountId(), filter);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrderDetailResponse response = orderService.getOrderById(principal.getAccountId(), id);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> cancelOrder(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrderDetailResponse response = orderService.cancelOrder(principal.getAccountId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }
}
