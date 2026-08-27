package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.request.AssignAttributeRequest;
import com.ecm.server.dto.response.CategoryAttributeResponse;
import com.ecm.server.model.CategoryAttribute;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface CategoryAttributeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "categoryGroup", ignore = true)
    @Mapping(target = "attribute", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    CategoryAttribute toEntity(AssignAttributeRequest request);

    @Mapping(target = "categoryGroupId", source = "categoryGroup.id")
    @Mapping(target = "attributeId", source = "attribute.id")
    @Mapping(target = "attributeKey", source = "attribute.key")
    @Mapping(target = "attributeDisplayName", source = "attribute.displayName")
    CategoryAttributeResponse toResponse(CategoryAttribute entity);

    List<CategoryAttributeResponse> toResponseList(List<CategoryAttribute> entities);
}
