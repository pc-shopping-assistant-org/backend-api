package com.ecm.server.repository;

import com.ecm.server.model.CategoryAttributeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryAttributeGroupRepository extends JpaRepository<CategoryAttributeGroup, UUID> {

    @Query("""
        SELECT DISTINCT g FROM CategoryAttributeGroup g
        LEFT JOIN FETCH g.categoryAttributes ca
        LEFT JOIN FETCH ca.attribute a
        WHERE g.category.id = :categoryId
          AND g.status != :excludedStatus
          AND (ca.status IS NULL OR ca.status != :excludedStatus)
        ORDER BY g.displayOrder ASC
    """)
    List<CategoryAttributeGroup> findGroupsWithAttributesByCategoryId(
            @Param("categoryId") UUID categoryId,
            @Param("excludedStatus") String excludedStatus
    );

    List<CategoryAttributeGroup> findByCategoryIdAndStatusNotOrderByDisplayOrderAsc(UUID categoryId, String status);

    boolean existsByCategoryIdAndName(UUID categoryId, String name);
}
