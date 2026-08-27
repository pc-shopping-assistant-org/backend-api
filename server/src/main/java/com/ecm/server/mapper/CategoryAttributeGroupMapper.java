package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.request.CreateCategoryGroupRequest;
import com.ecm.server.dto.request.UpdateCategoryGroupRequest;
import com.ecm.server.dto.response.CategoryAttributeGroupResponse;
import com.ecm.server.model.CategoryAttributeGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface CategoryAttributeGroupMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "categoryAttributes", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    CategoryAttributeGroup toEntity(CreateCategoryGroupRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "categoryAttributes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(UpdateCategoryGroupRequest request, @MappingTarget CategoryAttributeGroup entity);

    @Mapping(target = "categoryId", source = "category.id")
    CategoryAttributeGroupResponse toResponse(CategoryAttributeGroup entity);

    List<CategoryAttributeGroupResponse> toResponseList(List<CategoryAttributeGroup> entities);
}
