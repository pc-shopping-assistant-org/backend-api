package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateReviewRequest;
import com.ecm.server.dto.request.ReviewFilterRequest;
import com.ecm.server.dto.request.UpdateReviewRequest;
import com.ecm.server.dto.response.ProductRatingSummaryResponse;
import com.ecm.server.dto.response.ReviewResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.ProductReviewMapper;
import com.ecm.server.model.Customer;
import com.ecm.server.model.Product;
import com.ecm.server.model.ProductReview;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.repository.ProductRepository;
import com.ecm.server.repository.ProductReviewRepository;
import com.ecm.server.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DELETED = "DELETED";
    public static final int DEFAULT_LIMIT = 20;

    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final ProductReviewMapper productReviewMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<ReviewResponse> getProductReviews(UUID productId, ReviewFilterRequest filter) {
        // 1. Verify product exists
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(StatusCode.PRODUCT_NOT_FOUND);
        }

        // 2. Query active reviews using keyset cursor pagination
        int pageSize = (filter.getLimit() != null && filter.getLimit() > 0) ? filter.getLimit() : DEFAULT_LIMIT;
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        List<ProductReview> reviews = (filter.getCursor() == null)
                ? productReviewRepository.findActiveReviewsInitial(productId, filter.getRating(), pageable)
                : productReviewRepository.findActiveReviewsAfterCursor(filter.getCursor(), productId, filter.getRating(), pageable);

        // 3. Assemble and return cursor response envelope
        return CursorPageResponse.of(
                reviews,
                pageSize,
                review -> review.getId().toString(),
                review -> {
                    ReviewResponse response = productReviewMapper.toResponse(review);
                    boolean isVerified = productReviewRepository.hasPurchasedProduct(review.getCustomer().getId(), productId);
                    response.setIsVerifiedPurchase(isVerified);
                    return response;
                }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProductRatingSummaryResponse getProductRatingSummary(UUID productId) {
        // 1. Verify product exists
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(StatusCode.PRODUCT_NOT_FOUND);
        }

        // 2. Fetch average rating and total review counts
        Double avgRating = productReviewRepository.getAverageRating(productId);
        Long totalCount = productReviewRepository.getTotalReviews(productId);

        // 3. Construct 1-5 star rating distribution map
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }

        List<Object[]> rawDist = productReviewRepository.getRatingDistribution(productId);
        for (Object[] row : rawDist) {
            Integer star = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            distribution.put(star, count);
        }

        // 4. Return rating summary DTO
        return ProductRatingSummaryResponse.builder()
                .productId(productId)
                .averageRating(Math.round(avgRating * 10.0) / 10.0)
                .totalReviews(totalCount)
                .ratingDistribution(distribution)
                .build();
    }

    @Override
    @Transactional
    public ReviewResponse createReview(UUID accountId, UUID productId, CreateReviewRequest request) {
        // 1. Retrieve customer profile from authenticated account
        Customer customer = customerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.CUSTOMER_NOT_FOUND));

        // 2. Validate product exists and is active
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(StatusCode.PRODUCT_NOT_FOUND));

        if (!STATUS_ACTIVE.equalsIgnoreCase(product.getStatus())) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Product is inactive or deleted");
        }

        // 3. Ensure customer has not already reviewed this product
        boolean alreadyReviewed = productReviewRepository.existsByCustomerIdAndProductIdAndStatusNot(customer.getId(), productId, STATUS_DELETED);
        if (alreadyReviewed) {
            throw new BusinessException(StatusCode.CONFLICT, "You have already reviewed this product. Please update your existing review instead.");
        }

        // 4. Check verified purchase status
        boolean isVerified = productReviewRepository.hasPurchasedProduct(customer.getId(), productId);

        // 5. Create and persist new review entity
        ProductReview review = ProductReview.builder()
                .product(product)
                .customer(customer)
                .rating(request.getRating())
                .comment(request.getComment() != null ? request.getComment().trim() : null)
                .status(STATUS_ACTIVE)
                .build();

        ProductReview savedReview = productReviewRepository.save(review);
        log.info("Customer [{}] submitted review [{}] for product [{}] with rating [{}]", customer.getId(), savedReview.getId(), productId, request.getRating());

        // 6. Assemble and return ReviewResponse
        ReviewResponse response = productReviewMapper.toResponse(savedReview);
        response.setIsVerifiedPurchase(isVerified);
        return response;
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(UUID accountId, UUID productId, UUID reviewId, UpdateReviewRequest request) {
        // 1. Retrieve customer profile
        Customer customer = customerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.CUSTOMER_NOT_FOUND));

        // 2. Retrieve review and enforce ownership
        ProductReview review = productReviewRepository.findByIdAndStatusNot(reviewId, STATUS_DELETED)
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Review not found"));

        if (!review.getProduct().getId().equals(productId)) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Review does not belong to the specified product");
        }

        if (!review.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessException(StatusCode.FORBIDDEN, "You do not have permission to modify this review");
        }

        // 3. Update rating and comment fields
        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment().trim());
        }

        ProductReview updatedReview = productReviewRepository.save(review);
        log.info("Customer [{}] updated review [{}] for product [{}]", customer.getId(), reviewId, productId);

        // 4. Return updated review response
        boolean isVerified = productReviewRepository.hasPurchasedProduct(customer.getId(), productId);
        ReviewResponse response = productReviewMapper.toResponse(updatedReview);
        response.setIsVerifiedPurchase(isVerified);
        return response;
    }

    @Override
    @Transactional
    public void deleteReview(UUID accountId, UUID productId, UUID reviewId) {
        // 1. Retrieve customer profile
        Customer customer = customerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.CUSTOMER_NOT_FOUND));

        // 2. Retrieve review and enforce ownership
        ProductReview review = productReviewRepository.findByIdAndStatusNot(reviewId, STATUS_DELETED)
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Review not found"));

        if (!review.getProduct().getId().equals(productId)) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "Review does not belong to the specified product");
        }

        if (!review.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessException(StatusCode.FORBIDDEN, "You do not have permission to delete this review");
        }

        // 3. Perform soft delete
        review.setStatus(STATUS_DELETED);
        productReviewRepository.save(review);
        log.info("Customer [{}] soft-deleted review [{}] for product [{}]", customer.getId(), reviewId, productId);
    }
}
