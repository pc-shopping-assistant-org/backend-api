package com.ecm.server.repository;

import com.ecm.server.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    boolean existsByName(String name);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<Supplier> findByStatusNot(String status);

    @Query("""
        SELECT s FROM Supplier s
        WHERE (:cursor IS NULL OR s.id < :cursor)
          AND (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:status IS NULL OR s.status = :status)
        ORDER BY s.id DESC
        LIMIT :queryLimit
    """)
    List<Supplier> findSuppliersByCursor(
            @Param("cursor") UUID cursor,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("queryLimit") int queryLimit
    );
}
