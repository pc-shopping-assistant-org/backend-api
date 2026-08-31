package com.ecm.server.repository;

import com.ecm.server.model.DiscountVariant;
import com.ecm.server.model.DiscountVariant.DiscountVariantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DiscountVariantRepository extends JpaRepository<DiscountVariant, DiscountVariantId> {
    @Query("SELECT dv FROM DiscountVariant dv JOIN FETCH dv.variant v JOIN FETCH v.product WHERE dv.discount.id = :discountId")
    List<DiscountVariant> findByDiscountIdDiscountId(@Param("discountId") UUID discountId);

    @Modifying
    @Query("DELETE FROM DiscountVariant dv WHERE dv.discount.id = :discountId")
    void deleteByDiscountId(@Param("discountId") UUID discountId);
}
