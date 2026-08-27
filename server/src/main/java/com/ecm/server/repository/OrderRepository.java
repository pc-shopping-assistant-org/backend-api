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

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.customer.id = :customerId AND o.status = 'COMPLETED'")
    Long sumSpentByCustomerId(@Param("customerId") UUID customerId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'COMPLETED'")
    Long sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'COMPLETED' AND o.orderTime >= :from AND o.orderTime <= :to")
    Long sumTotalRevenueBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderTime >= :from AND o.orderTime <= :to")
    Long countOrdersBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();

    @Query("SELECT o FROM Order o WHERE o.status = 'COMPLETED' AND o.orderTime >= :from AND o.orderTime <= :to ORDER BY o.orderTime ASC")
    List<Order> findCompletedOrdersBetween(@Param("from") Instant from, @Param("to") Instant to);

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
