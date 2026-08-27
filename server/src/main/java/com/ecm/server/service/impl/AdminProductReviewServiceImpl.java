package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.ReviewFilterRequest;
import com.ecm.server.dto.request.UpdateStatusRequest;
import com.ecm.server.dto.response.ReviewResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.ProductReviewMapper;
import com.ecm.server.model.ProductReview;
import com.ecm.server.repository.ProductReviewRepository;
import com.ecm.server.service.AdminProductReviewService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductReviewServiceImpl implements AdminProductReviewService {

    public static final int DEFAULT_LIMIT = 20;

    private final ProductReviewRepository productReviewRepository;
    private final ProductReviewMapper productReviewMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<ReviewResponse> getAdminReviews(ReviewFilterRequest filter) {
        // 1. Prepare pagination and filtering parameters
        int pageSize = (filter.getLimit() != null && filter.getLimit() > 0) ? filter.getLimit() : DEFAULT_LIMIT;
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        // 2. Build dynamic specification with eager fetch joins
        Specification<ProductReview> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getCursor() != null) {
                predicates.add(cb.lessThan(root.get("id"), filter.getCursor()));
            }
            if (filter.getProductId() != null) {
                predicates.add(cb.equal(root.get("product").get("id"), filter.getProductId()));
            }
            if (filter.getRating() != null) {
                predicates.add(cb.equal(root.get("rating"), filter.getRating()));
            }
            if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), filter.getStatus().trim().toUpperCase()));
            }
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String search = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("comment")), search),
                        cb.like(cb.lower(root.get("customer").get("fullName")), search),
                        cb.like(cb.lower(root.get("product").get("name")), search)
                ));
            }

            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("customer", JoinType.LEFT);
                root.fetch("product", JoinType.LEFT);
            }
            query.orderBy(cb.desc(root.get("id")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<ProductReview> reviews = productReviewRepository.findAll(spec, pageable).getContent();

        // 3. Assemble and return cursor response envelope
        return CursorPageResponse.of(
                reviews,
                pageSize,
                review -> review.getId().toString(),
                review -> {
                    ReviewResponse response = productReviewMapper.toResponse(review);
                    boolean isVerified = productReviewRepository.hasPurchasedProduct(review.getCustomer().getId(), review.getProduct().getId());
                    response.setIsVerifiedPurchase(isVerified);
                    return response;
                }
        );
    }

    @Override
    @Transactional
    public ReviewResponse updateReviewStatus(UUID reviewId, UpdateStatusRequest request, UUID adminId) {
        // 1. Retrieve review entity
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Review not found"));

        // 2. Apply status update
        String targetStatus = request.getStatus().toUpperCase();
        review.setStatus(targetStatus);
        ProductReview savedReview = productReviewRepository.save(review);

        log.info("Admin [{}] updated review [{}] status to [{}] with reason [{}]", adminId, reviewId, targetStatus, request.getReason());

        // 3. Return updated review response
        boolean isVerified = productReviewRepository.hasPurchasedProduct(savedReview.getCustomer().getId(), savedReview.getProduct().getId());
        ReviewResponse response = productReviewMapper.toResponse(savedReview);
        response.setIsVerifiedPurchase(isVerified);
        return response;
    }
}
