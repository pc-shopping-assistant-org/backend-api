package com.ecm.server.repository;

import com.ecm.server.model.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    long countByProductVariantId(UUID productVariantId);

    long countByProductVariantProductId(UUID productId);

    long countByDiscountId(UUID discountId);

    List<OrderItem> findByOrderId(UUID orderId);

    @Query("""
        SELECT oi FROM OrderItem oi
        LEFT JOIN FETCH oi.productVariant pv
        LEFT JOIN FETCH pv.product
        LEFT JOIN FETCH oi.discount
        WHERE oi.order.id = :orderId
    """)
    List<OrderItem> findByOrderIdWithDetails(@Param("orderId") UUID orderId);

    @Query("""
        SELECT pv.product.id,
               pv.product.name,
               pv.product.imageUrl,
               COALESCE(SUM(oi.quantity), 0),
               COALESCE(SUM(oi.quantity * oi.unitAmount - oi.discountAmount), 0)
        FROM OrderItem oi
        JOIN oi.order o
        JOIN oi.productVariant pv
        WHERE o.status = 'COMPLETED'
          AND (CAST(:fromDate AS timestamp) IS NULL OR o.orderTime >= :fromDate)
          AND (CAST(:toDate AS timestamp) IS NULL OR o.orderTime <= :toDate)
        GROUP BY pv.product.id, pv.product.name, pv.product.imageUrl
        ORDER BY SUM(oi.quantity) DESC
    """)
    List<Object[]> findTopSellingProducts(
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );
}
