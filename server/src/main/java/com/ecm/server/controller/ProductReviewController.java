package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.config.security.UserPrincipal;
import com.ecm.server.dto.request.CreateReviewRequest;
import com.ecm.server.dto.request.ReviewFilterRequest;
import com.ecm.server.dto.request.UpdateReviewRequest;
import com.ecm.server.dto.response.ProductRatingSummaryResponse;
import com.ecm.server.dto.response.ReviewResponse;
import com.ecm.server.service.ProductReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<ReviewResponse>>> getProductReviews(
            @PathVariable UUID productId,
            @Valid @ModelAttribute ReviewFilterRequest filter
    ) {
        CursorPageResponse<ReviewResponse> response = productReviewService.getProductReviews(productId, filter);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ProductRatingSummaryResponse>> getProductRatingSummary(
            @PathVariable UUID productId
    ) {
        ProductRatingSummaryResponse response = productReviewService.getProductRatingSummary(productId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable UUID productId,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ReviewResponse response = productReviewService.createReview(principal.getAccountId(), productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(StatusCode.CREATED, response));
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable UUID productId,
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ReviewResponse response = productReviewService.updateReview(principal.getAccountId(), productId, reviewId, request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.UPDATED, response));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable UUID productId,
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        productReviewService.deleteReview(principal.getAccountId(), productId, reviewId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.DELETED, null));
    }
}
