package com.ecm.server.repository;

import com.ecm.server.model.CategoryAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, UUID> {

    List<CategoryAttribute> findByCategoryGroupIdAndStatusNotOrderByDisplayOrderAsc(UUID categoryGroupId, String status);

    boolean existsByCategoryGroupIdAndAttributeId(UUID categoryGroupId, UUID attributeId);

    long countByAttributeId(UUID attributeId);
}
