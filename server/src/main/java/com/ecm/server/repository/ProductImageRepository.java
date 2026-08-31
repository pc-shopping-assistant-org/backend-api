package com.ecm.server.repository;

import com.ecm.server.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductVariantIdAndStatusNot(UUID productVariantId, String status);

    @Query("""
            SELECT pi FROM ProductImage pi
            JOIN FETCH pi.file f
            WHERE pi.productVariant.product.id = :productId
              AND pi.status = 'ACTIVE'
            ORDER BY pi.isMain DESC, pi.createdAt ASC
            """)
    List<ProductImage> findActiveForProduct(@Param("productId") UUID productId);
}
