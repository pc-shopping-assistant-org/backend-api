package com.ecm.server.repository;

import com.ecm.server.model.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT o FROM Order o WHERE o.customer.accountId = :customerId ORDER BY o.orderTime DESC")
    List<Order> findByCustomerIdOrderByOrderTimeDesc(@Param("customerId") UUID customerId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.accountId = :customerId")
    long countByCustomerId(@Param("customerId") UUID customerId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDiscount.id = :discountId")
    long countByOrderDiscountId(@Param("discountId") UUID discountId);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.customer.accountId = :customerId AND o.status = 'COMPLETED'")
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
                LEFT JOIN FETCH o.orderDiscount
                WHERE o.id = :id
            """)
    Optional<Order> findByIdWithDetails(@Param("id") UUID id);

    /** Lock an order before a state/stock mutation is applied. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT o FROM Order o
                LEFT JOIN FETCH o.customer c
                LEFT JOIN FETCH c.account
                LEFT JOIN FETCH o.orderDiscount
                WHERE o.id = :id
            """)
    Optional<Order> findByIdWithDetailsForUpdate(@Param("id") UUID id);

    @Query("""
                SELECT o FROM Order o
                LEFT JOIN FETCH o.customer c
                LEFT JOIN FETCH o.orderDiscount
                WHERE o.customer.accountId = :customerId
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
                LEFT JOIN FETCH o.orderDiscount
                WHERE o.customer.accountId = :customerId
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

    /** Resolve a customer-owned UUID/invoice prefix without loading all orders. */
    @Query(value = """
            SELECT o.*
            FROM orders o
            WHERE o.customer_id = :customerId
              AND (:status IS NULL OR o.status = :status)
              AND LOWER(REPLACE(CAST(o.id AS text), '-', ''))
                  LIKE LOWER(CONCAT(:identifier, '%'))
            ORDER BY o.id DESC
            """, nativeQuery = true)
    List<Order> findCustomerOrdersByIdentifierInitial(
            @Param("customerId") UUID customerId,
            @Param("identifier") String identifier,
            @Param("status") String status,
            Pageable pageable
    );

    @Query(value = """
            SELECT o.*
            FROM orders o
            WHERE o.customer_id = :customerId
              AND o.id < :cursor
              AND (:status IS NULL OR o.status = :status)
              AND LOWER(REPLACE(CAST(o.id AS text), '-', ''))
                  LIKE LOWER(CONCAT(:identifier, '%'))
            ORDER BY o.id DESC
            """, nativeQuery = true)
    List<Order> findCustomerOrdersByIdentifierAfterCursor(
            @Param("customerId") UUID customerId,
            @Param("identifier") String identifier,
            @Param("status") String status,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("""
                SELECT o FROM Order o
                LEFT JOIN FETCH o.customer c
                LEFT JOIN FETCH c.account
                LEFT JOIN FETCH o.orderDiscount
                WHERE (CAST(:customerId AS uuid) IS NULL OR o.customer.accountId = :customerId)
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
                LEFT JOIN FETCH o.orderDiscount
                WHERE o.id < :cursor
                  AND (CAST(:customerId AS uuid) IS NULL OR o.customer.accountId = :customerId)
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

    @Query("""
                SELECT DISTINCT o FROM Order o
                LEFT JOIN FETCH o.customer c
                LEFT JOIN FETCH c.account
                LEFT JOIN FETCH o.orderDiscount
                WHERE o.status = 'COMPLETED'
                  AND (:orderId IS NULL OR o.id = :orderId)
                  AND (:keyword IS NULL OR LOWER(c.firstName) LIKE :keyword
                       OR LOWER(c.lastName) LIKE :keyword
                       OR LOWER(c.account.email) LIKE :keyword
                       OR LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE :keyword)
                  AND (CAST(:fromDate AS timestamp) IS NULL OR o.orderTime >= :fromDate)
                  AND (CAST(:toDate AS timestamp) IS NULL OR o.orderTime <= :toDate)
                ORDER BY o.id DESC
            """)
    List<Order> findInvoicesInitial(
            @Param("orderId") UUID orderId,
            @Param("keyword") String keyword,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );

    @Query("""
                SELECT DISTINCT o FROM Order o
                LEFT JOIN FETCH o.customer c
                LEFT JOIN FETCH c.account
                LEFT JOIN FETCH o.orderDiscount
                WHERE o.status = 'COMPLETED'
                  AND o.id < :cursor
                  AND (:orderId IS NULL OR o.id = :orderId)
                  AND (:keyword IS NULL OR LOWER(c.firstName) LIKE :keyword
                       OR LOWER(c.lastName) LIKE :keyword
                       OR LOWER(c.account.email) LIKE :keyword
                       OR LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE :keyword)
                  AND (CAST(:fromDate AS timestamp) IS NULL OR o.orderTime >= :fromDate)
                  AND (CAST(:toDate AS timestamp) IS NULL OR o.orderTime <= :toDate)
                ORDER BY o.id DESC
            """)
    List<Order> findInvoicesAfterCursor(
            @Param("cursor") UUID cursor,
            @Param("orderId") UUID orderId,
            @Param("keyword") String keyword,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );

    /**
     * Invoice IDs are rendered as INV- plus an eight-character UUID prefix.
     * Resolve that read-model prefix in SQL instead of loading every order into
     * the application when the admin searches invoices.
     */
    @Query(value = """
            SELECT o.id
            FROM orders o
            WHERE o.status = 'COMPLETED'
              AND LOWER(REPLACE(o.id::text, '-', '')) LIKE LOWER(CONCAT(:prefix, '%'))
            ORDER BY o.id DESC
            """, nativeQuery = true)
    List<UUID> findCompletedOrderIdsByUuidPrefix(@Param("prefix") String prefix, Pageable pageable);
}
