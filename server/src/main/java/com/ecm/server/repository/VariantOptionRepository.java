package com.ecm.server.repository;

import com.ecm.server.model.VariantOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VariantOptionRepository extends JpaRepository<VariantOption, UUID> {

    long countByOptionId(UUID optionId);
}
