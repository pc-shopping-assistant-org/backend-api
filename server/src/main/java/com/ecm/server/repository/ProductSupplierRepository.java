package com.ecm.server.repository;

import com.ecm.server.model.ProductSupplier;
import com.ecm.server.model.ProductSupplier.ProductSupplierId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductSupplierRepository extends JpaRepository<ProductSupplier, ProductSupplierId> {
    @Query("SELECT COUNT(ps) FROM ProductSupplier ps WHERE ps.supplier.id = :supplierId")
    long countBySupplierId(@Param("supplierId") UUID supplierId);

    @Query("SELECT CASE WHEN COUNT(ps) > 0 THEN true ELSE false END FROM ProductSupplier ps WHERE ps.supplier.id = :supplierId")
    boolean existsBySupplierId(@Param("supplierId") UUID supplierId);

    @Modifying
    @Query("DELETE FROM ProductSupplier ps WHERE ps.product.id = :productId")
    void deleteByProductId(@Param("productId") UUID productId);
}
