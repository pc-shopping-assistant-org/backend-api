package com.ecm.server.repository;

import com.ecm.server.model.Discount;
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
public interface DiscountRepository extends JpaRepository<Discount, UUID> {

    Optional<Discount> findByCodeIgnoreCase(String code);

    Optional<Discount> findByCodeIgnoreCaseAndStatus(String code, String status);

    boolean existsByCodeIgnoreCase(String code);

    @Query("""
                SELECT d FROM Discount d
                LEFT JOIN FETCH d.discountProductVariants dpv
                LEFT JOIN FETCH dpv.productVariant pv
                LEFT JOIN FETCH pv.product
                WHERE d.id = :id AND d.status <> :deletedStatus
            """)
    Optional<Discount> findByIdWithDetails(@Param("id") UUID id, @Param("deletedStatus") String deletedStatus);

    @Query("""
                SELECT d FROM Discount d
                WHERE d.status = 'ACTIVE'
                  AND d.startAt <= :now
                  AND d.endAt >= :now
                  AND (:type IS NULL OR d.type = :type)
                  AND (:scope IS NULL OR d.scope = :scope)
                  AND (:keyword IS NULL OR LOWER(d.code) LIKE :keyword OR LOWER(d.title) LIKE :keyword)
                ORDER BY d.id DESC
            """)
    List<Discount> findActiveInitial(
            @Param("now") Instant now,
            @Param("type") String type,
            @Param("scope") String scope,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
                SELECT d FROM Discount d
                WHERE d.id < :cursor
                  AND d.status = 'ACTIVE'
                  AND d.startAt <= :now
                  AND d.endAt >= :now
                  AND (:type IS NULL OR d.type = :type)
                  AND (:scope IS NULL OR d.scope = :scope)
                  AND (:keyword IS NULL OR LOWER(d.code) LIKE :keyword OR LOWER(d.title) LIKE :keyword)
                ORDER BY d.id DESC
            """)
    List<Discount> findActiveAfterCursor(
            @Param("cursor") UUID cursor,
            @Param("now") Instant now,
            @Param("type") String type,
            @Param("scope") String scope,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
                SELECT d FROM Discount d
                WHERE (:status IS NULL AND d.status <> 'DELETED' OR d.status = :status)
                  AND (:type IS NULL OR d.type = :type)
                  AND (:scope IS NULL OR d.scope = :scope)
                  AND (:keyword IS NULL OR LOWER(d.code) LIKE :keyword OR LOWER(d.title) LIKE :keyword)
                ORDER BY d.id DESC
            """)
    List<Discount> findAdminInitial(
            @Param("status") String status,
            @Param("type") String type,
            @Param("scope") String scope,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
                SELECT d FROM Discount d
                WHERE d.id < :cursor
                  AND (:status IS NULL AND d.status <> 'DELETED' OR d.status = :status)
                  AND (:type IS NULL OR d.type = :type)
                  AND (:scope IS NULL OR d.scope = :scope)
                  AND (:keyword IS NULL OR LOWER(d.code) LIKE :keyword OR LOWER(d.title) LIKE :keyword)
                ORDER BY d.id DESC
            """)
    List<Discount> findAdminAfterCursor(
            @Param("cursor") UUID cursor,
            @Param("status") String status,
            @Param("type") String type,
            @Param("scope") String scope,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
