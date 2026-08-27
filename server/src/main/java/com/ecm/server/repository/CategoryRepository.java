package com.ecm.server.repository;

import com.ecm.server.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySeoName(String seoName);

    boolean existsByName(String name);

    boolean existsBySeoName(String seoName);

    List<Category> findByStatusNot(String status);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL AND c.status != :excludedStatus")
    List<Category> findRootCategoriesWithChildren(String excludedStatus);

    long countByParentIdAndStatusNot(UUID parentId, String status);
}
