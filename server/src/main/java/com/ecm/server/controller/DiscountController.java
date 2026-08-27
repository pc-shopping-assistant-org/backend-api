package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.DiscountFilterRequest;
import com.ecm.server.dto.request.ValidateDiscountRequest;
import com.ecm.server.dto.response.DiscountSummaryResponse;
import com.ecm.server.dto.response.DiscountValidationResponse;
import com.ecm.server.service.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/discounts")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService discountService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<DiscountSummaryResponse>>> getActiveDiscounts(
            @ModelAttribute DiscountFilterRequest filter
    ) {
        CursorPageResponse<DiscountSummaryResponse> response = discountService.getActiveDiscounts(filter);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<DiscountValidationResponse>> validateDiscount(
            @Valid @RequestBody ValidateDiscountRequest request
    ) {
        DiscountValidationResponse response = discountService.validateDiscount(request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }
}
