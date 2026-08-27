package com.ecm.server.repository;

import com.ecm.server.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    Optional<ProductVariant> findBySku(String sku);

    @Query("""
        SELECT DISTINCT v FROM ProductVariant v
        LEFT JOIN FETCH v.images img
        LEFT JOIN FETCH v.variantOptions vo
        LEFT JOIN FETCH vo.option o
        WHERE v.product.id = :productId
          AND v.status != :status
        ORDER BY v.price ASC
    """)
    List<ProductVariant> findByProductIdWithDetails(@Param("productId") UUID productId, @Param("status") String status);

    @Query("""
        SELECT DISTINCT v FROM ProductVariant v
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
        LEFT JOIN FETCH v.images img
        LEFT JOIN FETCH v.variantOptions vo
        LEFT JOIN FETCH vo.option o
        WHERE v.id = :id
          AND v.status != :status
    """)
    Optional<ProductVariant> findByIdWithDetails(@Param("id") UUID id, @Param("status") String status);

    long countByProductId(UUID productId);
}
