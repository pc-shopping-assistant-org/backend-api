package com.ecm.server.repository;

import com.ecm.server.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
