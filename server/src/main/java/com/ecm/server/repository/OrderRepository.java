package com.ecm.server.repository;

import com.ecm.server.model.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerIdOrderByOrderTimeDesc(UUID customerId);

    long countByCustomerId(UUID customerId);

    long countByDiscountId(UUID discountId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.customer.id = :customerId AND o.status = 'COMPLETED'")
    Long sumSpentByCustomerId(@Param("customerId") UUID customerId);

    @Query("""
        SELECT o FROM Order o
        LEFT JOIN FETCH o.customer c
        LEFT JOIN FETCH c.account
        LEFT JOIN FETCH o.discount
        WHERE o.id = :id
    """)
    Optional<Order> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
        SELECT o FROM Order o
        LEFT JOIN FETCH o.customer c
        LEFT JOIN FETCH o.discount
        WHERE o.customer.id = :customerId
          AND (:status IS NULL OR o.status = :status)
        ORDER BY o.id DESC
    """)
    List<Order> findCustomerOrdersInitial(
            @Param("customerId") UUID customerId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
        SELECT o FROM Order o
        LEFT JOIN FETCH o.customer c
        LEFT JOIN FETCH o.discount
        WHERE o.customer.id = :customerId
          AND o.id < :cursor
          AND (:status IS NULL OR o.status = :status)
        ORDER BY o.id DESC
    """)
    List<Order> findCustomerOrdersAfterCursor(
            @Param("customerId") UUID customerId,
            @Param("cursor") UUID cursor,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
        SELECT o FROM Order o
        LEFT JOIN FETCH o.customer c
        LEFT JOIN FETCH c.account
        LEFT JOIN FETCH o.discount
        WHERE (CAST(:customerId AS uuid) IS NULL OR o.customer.id = :customerId)
          AND (:status IS NULL OR o.status = :status)
          AND (CAST(:fromDate AS timestamp) IS NULL OR o.orderTime >= :fromDate)
          AND (CAST(:toDate AS timestamp) IS NULL OR o.orderTime <= :toDate)
        ORDER BY o.id DESC
    """)
    List<Order> findAdminOrdersInitial(
            @Param("customerId") UUID customerId,
            @Param("status") String status,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );

    @Query("""
        SELECT o FROM Order o
        LEFT JOIN FETCH o.customer c
        LEFT JOIN FETCH c.account
        LEFT JOIN FETCH o.discount
        WHERE o.id < :cursor
          AND (CAST(:customerId AS uuid) IS NULL OR o.customer.id = :customerId)
          AND (:status IS NULL OR o.status = :status)
          AND (CAST(:fromDate AS timestamp) IS NULL OR o.orderTime >= :fromDate)
          AND (CAST(:toDate AS timestamp) IS NULL OR o.orderTime <= :toDate)
        ORDER BY o.id DESC
    """)
    List<Order> findAdminOrdersAfterCursor(
            @Param("cursor") UUID cursor,
            @Param("customerId") UUID customerId,
            @Param("status") String status,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );
}
