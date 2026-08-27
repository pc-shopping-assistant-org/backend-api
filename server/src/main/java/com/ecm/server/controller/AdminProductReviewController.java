package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.config.security.UserPrincipal;
import com.ecm.server.dto.request.ReviewFilterRequest;
import com.ecm.server.dto.request.UpdateStatusRequest;
import com.ecm.server.dto.response.ReviewResponse;
import com.ecm.server.service.AdminProductReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE', 'ROLE_MANAGER')")
public class AdminProductReviewController {

    private final AdminProductReviewService adminProductReviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<ReviewResponse>>> getAdminReviews(
            @ModelAttribute ReviewFilterRequest filter
    ) {
        CursorPageResponse<ReviewResponse> response = adminProductReviewService.getAdminReviews(filter);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReviewStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID adminId = (principal != null) ? principal.getAccountId() : null;
        ReviewResponse response = adminProductReviewService.updateReviewStatus(id, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }
}
