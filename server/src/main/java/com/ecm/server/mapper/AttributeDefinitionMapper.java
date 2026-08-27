package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.request.CreateAttributeDefinitionRequest;
import com.ecm.server.dto.request.UpdateAttributeDefinitionRequest;
import com.ecm.server.dto.response.AttributeDefinitionResponse;
import com.ecm.server.model.AttributeDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface AttributeDefinitionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    AttributeDefinition toEntity(CreateAttributeDefinitionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "key", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(UpdateAttributeDefinitionRequest request, @MappingTarget AttributeDefinition entity);

    AttributeDefinitionResponse toResponse(AttributeDefinition entity);

    List<AttributeDefinitionResponse> toResponseList(List<AttributeDefinition> entities);
}
