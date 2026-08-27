package com.ecm.server.repository;

import com.ecm.server.model.DiscountProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiscountProductVariantRepository extends JpaRepository<DiscountProductVariant, UUID> {

    List<DiscountProductVariant> findByDiscountIdAndStatus(UUID discountId, String status);

    List<DiscountProductVariant> findByDiscountId(UUID discountId);

    boolean existsByDiscountIdAndProductVariantId(UUID discountId, UUID productVariantId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM DiscountProductVariant dpv WHERE dpv.discount.id = :discountId")
    void deleteByDiscountId(@Param("discountId") UUID discountId);
}
