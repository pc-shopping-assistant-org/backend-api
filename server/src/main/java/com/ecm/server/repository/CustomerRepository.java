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

    @Query("SELECT c FROM Customer c JOIN FETCH c.account a WHERE LOWER(a.email) = LOWER(:email)")
    Optional<Customer> findByEmail(@Param("email") String email);

    @Query("SELECT c FROM Customer c JOIN FETCH c.account a WHERE a.phone = :phone")
    Optional<Customer> findByPhone(@Param("phone") String phone);

    @Query("SELECT COUNT(c) > 0 FROM Customer c JOIN c.account a WHERE LOWER(a.email) = LOWER(:email)")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT COUNT(c) > 0 FROM Customer c JOIN c.account a WHERE a.phone = :phone")
    boolean existsByPhone(@Param("phone") String phone);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.createdAt >= :from AND c.createdAt <= :to")
    Long countNewCustomersBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
                SELECT c FROM Customer c
                JOIN FETCH c.account a
                LEFT JOIN FETCH a.role r
                WHERE (:keyword IS NULL OR LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE :keyword OR LOWER(a.email) LIKE :keyword OR LOWER(a.phone) LIKE :keyword)
                  AND (:status IS NULL OR a.status = :status)
                ORDER BY c.accountId DESC
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
                WHERE c.accountId < :cursor
                  AND (:keyword IS NULL OR LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE :keyword OR LOWER(a.email) LIKE :keyword OR LOWER(a.phone) LIKE :keyword)
                  AND (:status IS NULL OR a.status = :status)
                ORDER BY c.accountId DESC
            """)
    List<Customer> findCustomersAfterCursor(
            @Param("cursor") UUID cursor,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );
}
