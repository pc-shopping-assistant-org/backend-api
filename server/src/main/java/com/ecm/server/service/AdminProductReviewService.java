package com.ecm.server.service;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.ReviewFilterRequest;
import com.ecm.server.dto.request.UpdateStatusRequest;
import com.ecm.server.dto.response.ReviewResponse;

import java.util.UUID;

public interface AdminProductReviewService {

    CursorPageResponse<ReviewResponse> getAdminReviews(ReviewFilterRequest filter);

    ReviewResponse updateReviewStatus(UUID reviewId, UpdateStatusRequest request, UUID adminId);
}
