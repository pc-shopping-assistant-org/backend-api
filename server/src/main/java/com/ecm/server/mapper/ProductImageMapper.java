package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.request.CreateProductImageRequest;
import com.ecm.server.dto.response.ProductImageResponse;
import com.ecm.server.model.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collection;
import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface ProductImageMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productVariant", ignore = true)
    @Mapping(target = "file", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    ProductImage toEntity(CreateProductImageRequest request);

    @Mapping(target = "productVariantId", source = "productVariant.id")
    @Mapping(target = "imageUrl", source = "file.publicUrl")
    ProductImageResponse toResponse(ProductImage entity);

    List<ProductImageResponse> toResponseList(Collection<ProductImage> entities);
}
