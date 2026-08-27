package com.ecm.server.repository;

import com.ecm.server.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    @Query("""
        SELECT c FROM Customer c
        JOIN FETCH c.account a
        LEFT JOIN FETCH a.role r
        WHERE (:cursor IS NULL OR c.id < :cursor)
          AND (:keyword IS NULL OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:status IS NULL OR c.status = :status)
        ORDER BY c.id DESC
        LIMIT :queryLimit
    """)
    List<Customer> findCustomersByCursor(
            @Param("cursor") UUID cursor,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("queryLimit") int queryLimit
    );
}
