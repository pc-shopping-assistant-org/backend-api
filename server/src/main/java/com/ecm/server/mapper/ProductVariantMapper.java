package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.request.CreateProductVariantRequest;
import com.ecm.server.dto.request.UpdateProductVariantRequest;
import com.ecm.server.dto.response.OptionResponse;
import com.ecm.server.dto.response.ProductVariantResponse;
import com.ecm.server.model.ProductVariant;
import com.ecm.server.model.VariantOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;

@Mapper(config = GlobalMapperConfig.class, uses = {ProductImageMapper.class, OptionMapper.class})
public interface ProductVariantMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "variantOptions", ignore = true)
    ProductVariant toEntity(CreateProductVariantRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "variantOptions", ignore = true)
    void updateEntityFromRequest(UpdateProductVariantRequest request, @MappingTarget ProductVariant entity);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "options", source = "variantOptions", qualifiedByName = "mapVariantOptions")
    @Mapping(target = "images", source = "images")
    ProductVariantResponse toResponse(ProductVariant entity);

    List<ProductVariantResponse> toResponseList(List<ProductVariant> entities);

    @Named("mapVariantOptions")
    default List<OptionResponse> mapVariantOptions(java.util.Collection<VariantOption> variantOptions) {
        if (variantOptions == null) {
            return Collections.emptyList();
        }
        return variantOptions.stream()
                .filter(vo -> vo.getOption() != null)
                .map(vo -> OptionResponse.builder()
                        .id(vo.getOption().getId())
                        .type(vo.getOption().getType())
                        .name(vo.getOption().getName())
                        .value(vo.getOption().getValue())
                        .status(vo.getOption().getStatus())
                        .createdAt(vo.getOption().getCreatedAt())
                        .build())
                .toList();
    }
}
