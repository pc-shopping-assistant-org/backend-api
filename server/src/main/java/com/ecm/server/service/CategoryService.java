package com.ecm.server.service;

import com.ecm.server.dto.request.CreateCategoryRequest;
import com.ecm.server.dto.request.UpdateCategoryRequest;
import com.ecm.server.dto.response.CategoryResponse;
import com.ecm.server.dto.response.CategoryTreeResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    List<CategoryTreeResponse> getCategoryTree();

    CategoryResponse getCategoryById(UUID id);

    CategoryResponse getCategoryBySlug(String seoName);

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request);

    void deleteCategory(UUID id);
}
