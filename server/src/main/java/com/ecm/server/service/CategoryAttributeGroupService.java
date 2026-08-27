package com.ecm.server.service;

import com.ecm.server.dto.request.AssignAttributeRequest;
import com.ecm.server.dto.request.CreateCategoryGroupRequest;
import com.ecm.server.dto.request.UpdateCategoryGroupRequest;
import com.ecm.server.dto.response.CategoryAttributeGroupResponse;
import com.ecm.server.dto.response.CategoryAttributeResponse;
import com.ecm.server.dto.response.CategorySpecsSchemaResponse;

import java.util.UUID;

public interface CategoryAttributeGroupService {

    CategorySpecsSchemaResponse getCategorySpecsSchema(UUID categoryId);

    CategoryAttributeGroupResponse createGroup(CreateCategoryGroupRequest request);

    CategoryAttributeGroupResponse updateGroup(UUID groupId, UpdateCategoryGroupRequest request);

    CategoryAttributeResponse assignAttribute(AssignAttributeRequest request);

    void unassignAttribute(UUID assignmentId);
}
