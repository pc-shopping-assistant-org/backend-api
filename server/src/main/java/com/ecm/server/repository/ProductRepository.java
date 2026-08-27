package com.ecm.server.repository;

import com.ecm.server.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    long countByCategoryId(UUID categoryId);

    long countByBrandId(UUID brandId);

    long countBySupplierId(UUID supplierId);
}
