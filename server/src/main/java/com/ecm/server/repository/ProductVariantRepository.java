package com.ecm.server.repository;

import com.ecm.server.model.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    /** Lock the stock row while checkout calculates and decrements inventory. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product p WHERE v.id = :id AND v.status = 'ACTIVE' AND p.status = 'ACTIVE'")
    Optional<ProductVariant> findActiveByIdForUpdate(@Param("id") UUID id);

    /** Lock a stock row for compensating inventory changes (for example order cancellation). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") UUID id);

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    Optional<ProductVariant> findBySku(String sku);

    @Query("""
                SELECT v FROM ProductVariant v
                LEFT JOIN FETCH v.product
                WHERE v.id = :id
            """)
    Optional<ProductVariant> findByIdWithProduct(@Param("id") UUID id);

    @Query("""
                SELECT DISTINCT v FROM ProductVariant v
                LEFT JOIN FETCH v.product
                LEFT JOIN FETCH v.images img
                LEFT JOIN FETCH v.variantOptions vo
                LEFT JOIN FETCH vo.option o
                WHERE v.product.id = :productId
                  AND v.status != :status
                ORDER BY v.listPrice ASC
            """)
    List<ProductVariant> findByProductIdWithDetails(@Param("productId") UUID productId, @Param("status") String status);

    @Query("""
                SELECT DISTINCT v FROM ProductVariant v
                LEFT JOIN FETCH v.product
                LEFT JOIN FETCH v.images img
                LEFT JOIN FETCH v.variantOptions vo
                LEFT JOIN FETCH vo.option o
                WHERE v.product.id = :productId
                  AND v.status = 'ACTIVE'
                ORDER BY v.listPrice ASC
            """)
    List<ProductVariant> findActiveByProductIdWithDetails(@Param("productId") UUID productId);

    @Query("""
                SELECT DISTINCT v FROM ProductVariant v
                LEFT JOIN FETCH v.product
                LEFT JOIN FETCH v.images img
                LEFT JOIN FETCH v.variantOptions vo
                LEFT JOIN FETCH vo.option o
                WHERE v.id = :id
                  AND v.product.id = :productId
                  AND v.status != :status
            """)
    Optional<ProductVariant> findByIdAndProductIdWithDetails(
            @Param("id") UUID id,
            @Param("productId") UUID productId,
            @Param("status") String status
    );

    @Query("""
                SELECT DISTINCT v FROM ProductVariant v
                LEFT JOIN FETCH v.product
                LEFT JOIN FETCH v.images img
                LEFT JOIN FETCH v.variantOptions vo
                LEFT JOIN FETCH vo.option o
                WHERE v.id = :id
                  AND v.product.id = :productId
                  AND v.status = 'ACTIVE'
            """)
    Optional<ProductVariant> findActiveByIdAndProductIdWithDetails(
            @Param("id") UUID id,
            @Param("productId") UUID productId
    );

    @Query("""
                SELECT DISTINCT v FROM ProductVariant v
                LEFT JOIN FETCH v.product
                LEFT JOIN FETCH v.images img
                LEFT JOIN FETCH v.variantOptions vo
                LEFT JOIN FETCH vo.option o
                WHERE v.id = :id
                  AND v.status != :status
            """)
    Optional<ProductVariant> findByIdWithDetails(@Param("id") UUID id, @Param("status") String status);

    long countByProductId(UUID productId);

    @Query("SELECT v.id FROM ProductVariant v WHERE v.product.category.id IN :categoryIds AND v.status = 'ACTIVE' AND v.product.status = 'ACTIVE' AND v.product.category.status = 'ACTIVE'")
    List<UUID> findIdsByProductCategoryIds(@Param("categoryIds") Collection<UUID> categoryIds);
}
