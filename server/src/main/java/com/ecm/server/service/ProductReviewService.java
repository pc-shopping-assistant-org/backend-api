package com.ecm.server.service;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.CreateReviewRequest;
import com.ecm.server.dto.request.ReviewFilterRequest;
import com.ecm.server.dto.request.UpdateReviewRequest;
import com.ecm.server.dto.response.ProductRatingSummaryResponse;
import com.ecm.server.dto.response.ReviewResponse;

import java.util.UUID;

public interface ProductReviewService {

    CursorPageResponse<ReviewResponse> getProductReviews(UUID productId, ReviewFilterRequest filter);

    ProductRatingSummaryResponse getProductRatingSummary(UUID productId);

    ReviewResponse createReview(UUID accountId, UUID productId, CreateReviewRequest request);

    ReviewResponse updateReview(UUID accountId, UUID productId, UUID reviewId, UpdateReviewRequest request);

    void deleteReview(UUID accountId, UUID productId, UUID reviewId);
}
