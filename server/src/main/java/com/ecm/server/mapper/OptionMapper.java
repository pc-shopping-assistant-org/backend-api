package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.request.CreateOptionRequest;
import com.ecm.server.dto.request.UpdateOptionRequest;
import com.ecm.server.dto.response.OptionResponse;
import com.ecm.server.model.Option;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface OptionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    Option toEntity(CreateOptionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(UpdateOptionRequest request, @MappingTarget Option entity);

    OptionResponse toResponse(Option entity);

    List<OptionResponse> toResponseList(List<Option> entities);
}
