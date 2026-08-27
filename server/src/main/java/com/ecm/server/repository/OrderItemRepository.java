package com.ecm.server.repository;

import com.ecm.server.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    long countByProductVariantId(UUID productVariantId);

    long countByProductVariantProductId(UUID productId);
}
