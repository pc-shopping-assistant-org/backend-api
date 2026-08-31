package com.ecm.server.repository;

import com.ecm.server.model.DiscountCategory;
import com.ecm.server.model.DiscountCategory.DiscountCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DiscountCategoryRepository extends JpaRepository<DiscountCategory, DiscountCategoryId> {
    @Query("SELECT dc FROM DiscountCategory dc JOIN FETCH dc.category WHERE dc.discount.id = :discountId")
    List<DiscountCategory> findByDiscountIdDiscountId(@Param("discountId") UUID discountId);

    @Modifying
    @Query("DELETE FROM DiscountCategory dc WHERE dc.discount.id = :discountId")
    void deleteByDiscountId(@Param("discountId") UUID discountId);
}
