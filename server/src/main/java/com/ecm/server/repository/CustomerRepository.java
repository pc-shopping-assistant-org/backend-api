package com.ecm.server.repository;

import com.ecm.server.model.Customer;
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
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByAccountId(UUID accountId);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.createdAt >= :from AND c.createdAt <= :to")
    Long countNewCustomersBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        SELECT c FROM Customer c
        JOIN FETCH c.account a
        LEFT JOIN FETCH a.role r
        WHERE (:keyword IS NULL OR LOWER(c.fullName) LIKE :keyword OR LOWER(c.email) LIKE :keyword OR LOWER(c.phone) LIKE :keyword OR LOWER(a.username) LIKE :keyword)
          AND (:status IS NULL OR c.status = :status)
        ORDER BY c.id DESC
    """)
    List<Customer> findCustomersInitial(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
        SELECT c FROM Customer c
        JOIN FETCH c.account a
        LEFT JOIN FETCH a.role r
        WHERE c.id < :cursor
          AND (:keyword IS NULL OR LOWER(c.fullName) LIKE :keyword OR LOWER(c.email) LIKE :keyword OR LOWER(c.phone) LIKE :keyword OR LOWER(a.username) LIKE :keyword)
          AND (:status IS NULL OR c.status = :status)
        ORDER BY c.id DESC
    """)
    List<Customer> findCustomersAfterCursor(
            @Param("cursor") UUID cursor,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );
}
