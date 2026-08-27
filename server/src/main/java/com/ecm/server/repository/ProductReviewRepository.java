package com.ecm.server.repository;

import com.ecm.server.model.ProductReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID>, JpaSpecificationExecutor<ProductReview> {

    Optional<ProductReview> findByIdAndStatusNot(UUID id, String status);

    boolean existsByCustomerIdAndProductIdAndStatusNot(UUID customerId, UUID productId, String status);

    Optional<ProductReview> findByCustomerIdAndProductIdAndStatusNot(UUID customerId, UUID productId, String status);

    @Query("""
                SELECT pr FROM ProductReview pr
                JOIN FETCH pr.customer c
                JOIN FETCH pr.product p
                WHERE pr.product.id = :productId
                  AND pr.status = 'ACTIVE'
                  AND (:rating IS NULL OR pr.rating = :rating)
                ORDER BY pr.id DESC
            """)
    List<ProductReview> findActiveReviewsInitial(
            @Param("productId") UUID productId,
            @Param("rating") Integer rating,
            Pageable pageable
    );

    @Query("""
                SELECT pr FROM ProductReview pr
                JOIN FETCH pr.customer c
                JOIN FETCH pr.product p
                WHERE pr.id < :cursor
                  AND pr.product.id = :productId
                  AND pr.status = 'ACTIVE'
                  AND (:rating IS NULL OR pr.rating = :rating)
                ORDER BY pr.id DESC
            """)
    List<ProductReview> findActiveReviewsAfterCursor(
            @Param("cursor") UUID cursor,
            @Param("productId") UUID productId,
            @Param("rating") Integer rating,
            Pageable pageable
    );

    @Query("SELECT COALESCE(AVG(pr.rating), 0.0) FROM ProductReview pr WHERE pr.product.id = :productId AND pr.status = 'ACTIVE'")
    Double getAverageRating(@Param("productId") UUID productId);

    @Query("SELECT COUNT(pr) FROM ProductReview pr WHERE pr.product.id = :productId AND pr.status = 'ACTIVE'")
    Long getTotalReviews(@Param("productId") UUID productId);

    @Query("SELECT pr.rating, COUNT(pr) FROM ProductReview pr WHERE pr.product.id = :productId AND pr.status = 'ACTIVE' GROUP BY pr.rating")
    List<Object[]> getRatingDistribution(@Param("productId") UUID productId);

    @Query("""
                SELECT COUNT(oi) > 0 FROM OrderItem oi
                JOIN oi.order o
                JOIN oi.productVariant pv
                WHERE o.customer.id = :customerId
                  AND pv.product.id = :productId
                  AND o.status = 'COMPLETED'
            """)
    boolean hasPurchasedProduct(@Param("customerId") UUID customerId, @Param("productId") UUID productId);
}
