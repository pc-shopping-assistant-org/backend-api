package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.request.CreateProductRequest;
import com.ecm.server.dto.request.UpdateProductRequest;
import com.ecm.server.dto.response.ProductDetailResponse;
import com.ecm.server.dto.response.ProductSummaryResponse;
import com.ecm.server.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class, uses = {BrandMapper.class, CategoryMapper.class, SupplierMapper.class, ProductVariantMapper.class})
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "variants", ignore = true)
    Product toEntity(CreateProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "variants", ignore = true)
    void updateEntityFromRequest(UpdateProductRequest request, @MappingTarget Product entity);

    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.name")
    @Mapping(target = "minPrice", ignore = true)
    @Mapping(target = "maxPrice", ignore = true)
    @Mapping(target = "ratingAverage", constant = "0.0")
    @Mapping(target = "reviewCount", constant = "0")
    ProductSummaryResponse toSummaryResponse(Product entity);

    @Mapping(target = "variants", source = "variants")
    @Mapping(target = "ratingAverage", constant = "0.0")
    @Mapping(target = "reviewCount", constant = "0")
    ProductDetailResponse toDetailResponse(Product entity);
}
