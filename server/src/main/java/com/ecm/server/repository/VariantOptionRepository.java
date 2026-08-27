package com.ecm.server.repository;

import com.ecm.server.model.VariantOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VariantOptionRepository extends JpaRepository<VariantOption, UUID> {

    long countByOptionId(UUID optionId);

    List<VariantOption> findByProductVariantId(UUID productVariantId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM VariantOption vo WHERE vo.productVariant.id = :productVariantId")
    void deleteByProductVariantId(@Param("productVariantId") UUID productVariantId);
}
