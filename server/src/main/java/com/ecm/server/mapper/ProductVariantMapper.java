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
import java.util.Collection;
import java.util.List;

@Mapper(config = GlobalMapperConfig.class, uses = {ProductImageMapper.class, OptionMapper.class})
public interface ProductVariantMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "listPrice", source = "listPrice")
    @Mapping(target = "warrantyMonths", expression = "java(parseWarrantyMonths(request.getWarranty()))")
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
    @Mapping(target = "listPrice", source = "listPrice")
    @Mapping(target = "warrantyMonths", expression = "java(request.getWarranty() == null || request.getWarranty().isBlank() ? entity.getWarrantyMonths() : parseWarrantyMonths(request.getWarranty()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "variantOptions", ignore = true)
    void updateEntityFromRequest(UpdateProductVariantRequest request, @MappingTarget ProductVariant entity);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "listPrice", source = "listPrice")
    @Mapping(target = "warranty", expression = "java(entity.getWarrantyMonths() == null ? null : entity.getWarrantyMonths() + \" months\")")
    @Mapping(target = "imageUrl", expression = "java(mainImageUrl(entity))")
    @Mapping(target = "options", source = "variantOptions", qualifiedByName = "mapVariantOptions")
    @Mapping(target = "images", source = "images", qualifiedByName = "mapActiveImages")
    ProductVariantResponse toResponse(ProductVariant entity);

    List<ProductVariantResponse> toResponseList(List<ProductVariant> entities);

    default Integer parseWarrantyMonths(String warranty) {
        if (warranty == null || warranty.isBlank()) {
            return 12;
        }
        String normalized = warranty.trim();
        if (!normalized.matches("^[1-9][0-9]*\\s*(?:(?i:months?)|tháng)?$")) {
            throw new IllegalArgumentException("Warranty must be a positive number of months");
        }
        try {
            return Integer.parseInt(normalized.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Warranty must be a positive number of months", ex);
        }
    }

    default String mainImageUrl(ProductVariant entity) {
        if (entity == null || entity.getImages() == null) {
            return null;
        }
        return entity.getImages().stream()
                .filter(image -> image != null && image.isMain()
                        && "ACTIVE".equalsIgnoreCase(image.getStatus())
                        && image.getFile() != null)
                .map(image -> image.getFile().getPublicUrl())
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElseGet(() -> entity.getImages().stream()
                        .filter(image -> image != null && "ACTIVE".equalsIgnoreCase(image.getStatus())
                                && image.getFile() != null)
                        .map(image -> image.getFile().getPublicUrl())
                        .filter(url -> url != null && !url.isBlank())
                        .findFirst()
                        .orElse(null));
    }

    @Named("mapVariantOptions")
    default List<OptionResponse> mapVariantOptions(java.util.Collection<VariantOption> variantOptions) {
        if (variantOptions == null) {
            return Collections.emptyList();
        }
        return variantOptions.stream()
                .filter(vo -> vo != null
                        && "ACTIVE".equalsIgnoreCase(vo.getStatus())
                        && vo.getOption() != null
                        && "ACTIVE".equalsIgnoreCase(vo.getOption().getStatus()))
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

    /**
     * Catalog responses must never expose inactive/deleted gallery rows.  The
     * entity relationship is intentionally broad because admin screens need
     * to inspect historical media, so the public response projection filters
     * it at the mapping boundary.
     */
    @Named("mapActiveImages")
    default List<com.ecm.server.dto.response.ProductImageResponse> mapActiveImages(
            Collection<com.ecm.server.model.ProductImage> images
    ) {
        if (images == null) {
            return Collections.emptyList();
        }
        return images.stream()
                .filter(image -> image != null && "ACTIVE".equalsIgnoreCase(image.getStatus()))
                .map(this::toImageResponse)
                .toList();
    }

    default com.ecm.server.dto.response.ProductImageResponse toImageResponse(
            com.ecm.server.model.ProductImage image
    ) {
        return com.ecm.server.dto.response.ProductImageResponse.builder()
                .id(image.getId())
                .name(image.getName())
                .productVariantId(image.getProductVariant() == null ? null : image.getProductVariant().getId())
                .imageUrl(image.getFile() == null ? null : image.getFile().getPublicUrl())
                .isMain(image.isMain())
                .status(image.getStatus())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
