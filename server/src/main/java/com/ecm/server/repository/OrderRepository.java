package com.ecm.server.repository;

import com.ecm.server.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerIdOrderByOrderTimeDesc(UUID customerId);

    long countByCustomerId(UUID customerId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.customer.id = :customerId AND o.status = 'COMPLETED'")
    Long sumSpentByCustomerId(@Param("customerId") UUID customerId);
}
