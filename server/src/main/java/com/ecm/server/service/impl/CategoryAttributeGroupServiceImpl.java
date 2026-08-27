package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.AssignAttributeRequest;
import com.ecm.server.dto.request.CreateCategoryGroupRequest;
import com.ecm.server.dto.request.UpdateCategoryGroupRequest;
import com.ecm.server.dto.response.CategoryAttributeGroupResponse;
import com.ecm.server.dto.response.CategoryAttributeResponse;
import com.ecm.server.dto.response.CategorySpecsSchemaResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.CategoryAttributeGroupMapper;
import com.ecm.server.mapper.CategoryAttributeMapper;
import com.ecm.server.model.AttributeDefinition;
import com.ecm.server.model.Category;
import com.ecm.server.model.CategoryAttribute;
import com.ecm.server.model.CategoryAttributeGroup;
import com.ecm.server.repository.AttributeDefinitionRepository;
import com.ecm.server.repository.CategoryAttributeGroupRepository;
import com.ecm.server.repository.CategoryAttributeRepository;
import com.ecm.server.repository.CategoryRepository;
import com.ecm.server.service.CategoryAttributeGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryAttributeGroupServiceImpl implements CategoryAttributeGroupService {

    public static final String STATUS_DELETED = "DELETED";

    private final CategoryRepository categoryRepository;
    private final CategoryAttributeGroupRepository categoryAttributeGroupRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final CategoryAttributeGroupMapper categoryAttributeGroupMapper;
    private final CategoryAttributeMapper categoryAttributeMapper;

    @Override
    @Transactional(readOnly = true)
    public CategorySpecsSchemaResponse getCategorySpecsSchema(UUID categoryId) {
        // 1. Verify category existence and status
        Category category = categoryRepository.findById(categoryId)
                .filter(c -> !STATUS_DELETED.equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_NOT_FOUND));

        // 2. Fetch attribute groups and assigned attributes for category
        List<CategoryAttributeGroup> groups = categoryAttributeGroupRepository.findGroupsWithAttributesByCategoryId(categoryId, STATUS_DELETED);

        // 3. Assemble dynamic schema response
        List<CategorySpecsSchemaResponse.GroupSchemaItem> groupItems = new ArrayList<>();
        for (CategoryAttributeGroup group : groups) {
            List<CategorySpecsSchemaResponse.AttributeSchemaItem> attrItems = new ArrayList<>();

            for (CategoryAttribute ca : group.getCategoryAttributes()) {
                if (ca.getStatus() != null && STATUS_DELETED.equalsIgnoreCase(ca.getStatus())) {
                    continue;
                }
                AttributeDefinition attr = ca.getAttribute();
                if (attr != null && !STATUS_DELETED.equalsIgnoreCase(attr.getStatus())) {
                    attrItems.add(CategorySpecsSchemaResponse.AttributeSchemaItem.builder()
                            .assignmentId(ca.getId())
                            .attributeId(attr.getId())
                            .key(attr.getKey())
                            .displayName(attr.getDisplayName())
                            .dataType(attr.getDataType())
                            .unit(attr.getUnit())
                            .allowedValues(attr.getAllowedValues())
                            .required(ca.isRequired())
                            .displayOrder(ca.getDisplayOrder())
                            .filterable(attr.isFilterable())
                            .comparable(attr.isComparable())
                            .build());
                }
            }

            groupItems.add(CategorySpecsSchemaResponse.GroupSchemaItem.builder()
                    .groupId(group.getId())
                    .groupName(group.getName())
                    .displayOrder(group.getDisplayOrder())
                    .attributes(attrItems)
                    .build());
        }

        // 4. Return category specs schema response DTO
        return CategorySpecsSchemaResponse.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .groups(groupItems)
                .build();
    }

    @Override
    @Transactional
    public CategoryAttributeGroupResponse createGroup(CreateCategoryGroupRequest request) {
        // 1. Validate parent category existence
        Category category = categoryRepository.findById(request.getCategoryId())
                .filter(c -> !STATUS_DELETED.equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_NOT_FOUND));

        // 2. Validate group name uniqueness within category
        if (categoryAttributeGroupRepository.existsByCategoryIdAndName(request.getCategoryId(), request.getName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Attribute group '" + request.getName() + "' already exists for this category");
        }

        // 3. Map DTO to entity via MapStruct and persist
        CategoryAttributeGroup group = categoryAttributeGroupMapper.toEntity(request);
        group.setCategory(category);
        CategoryAttributeGroup savedGroup = categoryAttributeGroupRepository.save(group);

        // 4. Return created group response DTO
        return categoryAttributeGroupMapper.toResponse(savedGroup);
    }

    @Override
    @Transactional
    public CategoryAttributeGroupResponse updateGroup(UUID groupId, UpdateCategoryGroupRequest request) {
        // 1. Fetch existing group entity
        CategoryAttributeGroup group = categoryAttributeGroupRepository.findById(groupId)
                .filter(g -> !STATUS_DELETED.equalsIgnoreCase(g.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_GROUP_NOT_FOUND));

        // 2. Validate name uniqueness within category if changed
        if (!group.getName().equalsIgnoreCase(request.getName())
                && categoryAttributeGroupRepository.existsByCategoryIdAndName(group.getCategory().getId(), request.getName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Attribute group '" + request.getName() + "' already exists for this category");
        }

        // 3. Update entity fields via MapStruct @MappingTarget
        categoryAttributeGroupMapper.updateEntityFromRequest(request, group);
        CategoryAttributeGroup updatedGroup = categoryAttributeGroupRepository.save(group);

        // 4. Return updated group response DTO
        return categoryAttributeGroupMapper.toResponse(updatedGroup);
    }

    @Override
    @Transactional
    public CategoryAttributeResponse assignAttribute(AssignAttributeRequest request) {
        // 1. Validate category group and attribute existence
        CategoryAttributeGroup group = categoryAttributeGroupRepository.findById(request.getCategoryGroupId())
                .filter(g -> !STATUS_DELETED.equalsIgnoreCase(g.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_GROUP_NOT_FOUND));

        AttributeDefinition attribute = attributeDefinitionRepository.findById(request.getAttributeId())
                .filter(a -> !STATUS_DELETED.equalsIgnoreCase(a.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.ATTRIBUTE_NOT_FOUND));

        // 2. Validate uniqueness of attribute assignment within group
        if (categoryAttributeRepository.existsByCategoryGroupIdAndAttributeId(request.getCategoryGroupId(), request.getAttributeId())) {
            throw new BusinessException(StatusCode.CONFLICT, "Attribute is already assigned to this specification group");
        }

        // 3. Map DTO to entity via MapStruct and persist
        CategoryAttribute categoryAttribute = categoryAttributeMapper.toEntity(request);
        categoryAttribute.setCategoryGroup(group);
        categoryAttribute.setAttribute(attribute);
        CategoryAttribute savedAssignment = categoryAttributeRepository.save(categoryAttribute);

        // 4. Return assignment response DTO
        return categoryAttributeMapper.toResponse(savedAssignment);
    }

    @Override
    @Transactional
    public void unassignAttribute(UUID assignmentId) {
        // 1. Retrieve assignment entity by ID
        CategoryAttribute categoryAttribute = categoryAttributeRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(StatusCode.CATEGORY_ATTRIBUTE_NOT_FOUND));

        // 2. Remove assignment from database
        categoryAttributeRepository.delete(categoryAttribute);
        log.info("Unassigned category attribute with assignment id: {}", assignmentId);
    }
}
