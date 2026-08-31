package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateCategoryRequest;
import com.ecm.server.dto.request.UpdateCategoryRequest;
import com.ecm.server.dto.response.CategoryResponse;
import com.ecm.server.dto.response.CategoryTreeResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.CategoryMapper;
import com.ecm.server.model.Category;
import com.ecm.server.repository.CategoryRepository;
import com.ecm.server.repository.ProductRepository;
import com.ecm.server.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    public static final String STATUS_DELETED = "DELETED";
    public static final String STATUS_ACTIVE = "ACTIVE";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategoryTree() {
        // 1. Fetch all active categories from database
        List<Category> allCategories = categoryRepository.findByStatusNot(STATUS_DELETED);

        // 2. Map all categories to CategoryTreeResponse DTOs and index by ID
        Map<UUID, CategoryTreeResponse> nodeMap = new HashMap<>();
        for (Category category : allCategories) {
            CategoryTreeResponse node = categoryMapper.toTreeResponse(category);
            node.setChildren(new ArrayList<>());
            nodeMap.put(category.getId(), node);
        }

        // 3. Assemble hierarchical tree structure
        List<CategoryTreeResponse> rootCategories = new ArrayList<>();
        for (Category category : allCategories) {
            CategoryTreeResponse currentNode = nodeMap.get(category.getId());
            if (category.getParent() == null) {
                rootCategories.add(currentNode);
            } else {
                CategoryTreeResponse parentNode = nodeMap.get(category.getParent().getId());
                if (parentNode != null) {
                    parentNode.getChildren().add(currentNode);
                } else {
                    rootCategories.add(currentNode);
                }
            }
        }

        return rootCategories;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        // 1. Find category entity by ID
        Category category = categoryRepository.findById(id)
                .filter(c -> !STATUS_DELETED.equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_NOT_FOUND));

        // 2. Map entity to response DTO via MapStruct
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String seoName) {
        // 1. Find category entity by SEO slug
        Category category = categoryRepository.findBySeoName(seoName)
                .filter(c -> !STATUS_DELETED.equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_NOT_FOUND));

        // 2. Map entity to response DTO via MapStruct
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        // 1. Check uniqueness of category name and SEO name
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Category with name '" + request.getName() + "' already exists");
        }
        if (categoryRepository.existsBySeoName(request.getSeoName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Category with SEO slug '" + request.getSeoName() + "' already exists");
        }

        // 2. Validate and resolve parent category if provided
        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .filter(c -> !STATUS_DELETED.equalsIgnoreCase(c.getStatus()))
                    .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_NOT_FOUND, "Parent category not found"));
        }

        // 3. Map DTO to entity via MapStruct and persist
        Category category = categoryMapper.toEntity(request);
        category.setParent(parent);
        Category savedCategory = categoryRepository.save(category);

        // 4. Map and return response DTO
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
        // 1. Find existing category entity
        Category category = categoryRepository.findById(id)
                .filter(c -> !STATUS_DELETED.equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_NOT_FOUND));

        // DELETED is reserved for deleteCategory(), which checks children and
        // products before soft-deleting the category.
        validateMutableStatus(request.getStatus());

        // 2. Validate SEO name uniqueness if changed
        if (!category.getName().equalsIgnoreCase(request.getName())
                && categoryRepository.existsByName(request.getName())) {
            throw new BusinessException(StatusCode.CONFLICT,
                    "Category with name '" + request.getName() + "' already exists");
        }
        if (!category.getSeoName().equals(request.getSeoName()) && categoryRepository.existsBySeoName(request.getSeoName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Category with SEO slug '" + request.getSeoName() + "' already exists");
        }

        // 3. Validate parent category circular dependency
        Category parent = null;
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BusinessException(StatusCode.CONFLICT, "A category cannot be its own parent");
            }
            parent = categoryRepository.findById(request.getParentId())
                    .filter(c -> !STATUS_DELETED.equalsIgnoreCase(c.getStatus()))
                    .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_NOT_FOUND, "Parent category not found"));
            if (wouldCreateCycle(id, parent)) {
                throw new BusinessException(StatusCode.CONFLICT,
                        "A category cannot be moved below one of its descendants");
            }
        }

        // 4. Update entity fields via MapStruct @MappingTarget
        categoryMapper.updateEntityFromRequest(request, category);
        category.setParent(parent);
        Category updatedCategory = categoryRepository.save(category);

        // 5. Return updated category response DTO
        return categoryMapper.toResponse(updatedCategory);
    }

    private boolean wouldCreateCycle(UUID categoryId, Category proposedParent) {
        Set<UUID> visited = new HashSet<>();
        Category current = proposedParent;
        while (current != null && current.getId() != null && visited.add(current.getId())) {
            if (categoryId.equals(current.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private void validateMutableStatus(String status) {
        if (status != null && !"ACTIVE".equalsIgnoreCase(status)
                && !"INACTIVE".equalsIgnoreCase(status)) {
            throw new BusinessException(StatusCode.BAD_REQUEST,
                    "Only ACTIVE or INACTIVE is allowed here; use the delete endpoint for DELETED");
        }
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        // 1. Find category entity
        Category category = categoryRepository.findById(id)
                .filter(c -> !STATUS_DELETED.equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_NOT_FOUND));

        // 2. Check foreign key constraints: subcategories
        long childCount = categoryRepository.countByParentIdAndStatusNot(id, STATUS_DELETED);
        if (childCount > 0) {
            throw new BusinessException(StatusCode.CONFLICT, "Cannot delete category containing active subcategories");
        }

        // 3. Check foreign key constraints: linked products
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new BusinessException(StatusCode.CONFLICT, "Cannot delete category associated with existing products");
        }

        // 4. Perform soft delete
        category.setStatus(STATUS_DELETED);
        categoryRepository.save(category);
        log.info("Soft deleted category with id: {}", id);
    }
}
