package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.response.ShippingMethodResponse;
import com.ecm.server.model.ShippingMethod;
import com.ecm.server.repository.ShippingMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shipping-methods")
@RequiredArgsConstructor
public class ShippingMethodController {

    private final ShippingMethodRepository shippingMethodRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShippingMethodResponse>>> getActiveShippingMethods() {
        List<ShippingMethodResponse> response = shippingMethodRepository
                .findAllByStatusIgnoreCaseOrderByFeeAscCodeAsc("ACTIVE")
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    private ShippingMethodResponse toResponse(ShippingMethod method) {
        return ShippingMethodResponse.builder()
                .id(method.getId())
                .code(method.getCode())
                .name(method.getName())
                .fee(method.getFee())
                .status(method.getStatus())
                .build();
    }
}
