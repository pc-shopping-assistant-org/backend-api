package com.ecm.server.repository;

import com.ecm.server.model.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    long countByCategoryId(UUID categoryId);

    long countByBrandId(UUID brandId);

    long countBySupplierId(UUID supplierId);

    boolean existsBySeoName(String seoName);

    Optional<Product> findBySeoNameAndStatusNot(String seoName, String status);

    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.brand
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.supplier
        WHERE p.id = :id AND p.status != :status
    """)
    Optional<Product> findDetailById(@Param("id") UUID id, @Param("status") String status);

    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.brand
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.supplier
        WHERE p.seoName = :seoName AND p.status != :status
    """)
    Optional<Product> findDetailBySeoName(@Param("seoName") String seoName, @Param("status") String status);

    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.brand
        LEFT JOIN FETCH p.category
        WHERE p.status = :status
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:brandId IS NULL OR p.brand.id = :brandId)
          AND (:keyword IS NULL OR LOWER(p.name) LIKE :keyword OR LOWER(p.seoName) LIKE :keyword)
        ORDER BY p.id DESC
    """)
    List<Product> findInitial(
            @Param("status") String status,
            @Param("categoryId") UUID categoryId,
            @Param("brandId") UUID brandId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.brand
        LEFT JOIN FETCH p.category
        WHERE p.status = :status
          AND p.id < :cursor
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:brandId IS NULL OR p.brand.id = :brandId)
          AND (:keyword IS NULL OR LOWER(p.name) LIKE :keyword OR LOWER(p.seoName) LIKE :keyword)
        ORDER BY p.id DESC
    """)
    List<Product> findAfterCursor(
            @Param("status") String status,
            @Param("cursor") UUID cursor,
            @Param("categoryId") UUID categoryId,
            @Param("brandId") UUID brandId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.brand
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.supplier
        WHERE (:status IS NULL OR p.status = :status)
          AND p.status != 'DELETED'
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:brandId IS NULL OR p.brand.id = :brandId)
          AND (:keyword IS NULL OR LOWER(p.name) LIKE :keyword OR LOWER(p.seoName) LIKE :keyword)
        ORDER BY p.id DESC
    """)
    List<Product> findAdminInitial(
            @Param("status") String status,
            @Param("categoryId") UUID categoryId,
            @Param("brandId") UUID brandId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.brand
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.supplier
        WHERE (:status IS NULL OR p.status = :status)
          AND p.status != 'DELETED'
          AND p.id < :cursor
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:brandId IS NULL OR p.brand.id = :brandId)
          AND (:keyword IS NULL OR LOWER(p.name) LIKE :keyword OR LOWER(p.seoName) LIKE :keyword)
        ORDER BY p.id DESC
    """)
    List<Product> findAdminAfterCursor(
            @Param("status") String status,
            @Param("cursor") UUID cursor,
            @Param("categoryId") UUID categoryId,
            @Param("brandId") UUID brandId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
