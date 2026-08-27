package com.ecm.server.repository;

import com.ecm.server.model.Supplier;
import org.springframework.data.domain.Pageable;
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
                WHERE (:keyword IS NULL OR LOWER(s.name) LIKE :keyword OR LOWER(s.email) LIKE :keyword OR LOWER(s.phone) LIKE :keyword)
                  AND (:status IS NULL OR s.status = :status)
                ORDER BY s.id DESC
            """)
    List<Supplier> findSuppliersInitial(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
                SELECT s FROM Supplier s
                WHERE s.id < :cursor
                  AND (:keyword IS NULL OR LOWER(s.name) LIKE :keyword OR LOWER(s.email) LIKE :keyword OR LOWER(s.phone) LIKE :keyword)
                  AND (:status IS NULL OR s.status = :status)
                ORDER BY s.id DESC
            """)
    List<Supplier> findSuppliersAfterCursor(
            @Param("cursor") UUID cursor,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );
}
