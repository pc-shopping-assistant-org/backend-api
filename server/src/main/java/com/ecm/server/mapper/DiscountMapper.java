package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.request.CreateDiscountRequest;
import com.ecm.server.dto.request.UpdateDiscountRequest;
import com.ecm.server.dto.response.DiscountDetailResponse;
import com.ecm.server.dto.response.DiscountSummaryResponse;
import com.ecm.server.dto.response.ProductVariantResponse;
import com.ecm.server.model.Discount;
import com.ecm.server.model.DiscountVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface DiscountMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "discountCategories", ignore = true)
    @Mapping(target = "discountVariants", ignore = true)
    Discount toEntity(CreateDiscountRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "discountCategories", ignore = true)
    @Mapping(target = "discountVariants", ignore = true)
    void updateEntityFromRequest(UpdateDiscountRequest request, @MappingTarget Discount entity);

    @Mapping(target = "discountType", source = "discountType")
    @Mapping(target = "applicationScope", source = "applicationScope")
    DiscountSummaryResponse toSummaryResponse(Discount entity);

    List<DiscountSummaryResponse> toSummaryResponseList(List<Discount> entities);

    @Mapping(target = "discountType", source = "discountType")
    @Mapping(target = "applicationScope", source = "applicationScope")
    @Mapping(target = "appliedVariants", expression = "java(mapDiscountVariants(entity.getDiscountVariants()))")
    @Mapping(target = "appliedCategoryIds", expression = "java(mapDiscountCategories(entity.getDiscountCategories()))")
    DiscountDetailResponse toDetailResponse(Discount entity);

    default List<ProductVariantResponse> mapDiscountVariants(Collection<DiscountVariant> variants) {
        if (variants == null) {
            return Collections.emptyList();
        }
        return variants.stream()
                .map(DiscountVariant::getVariant)
                .filter(java.util.Objects::nonNull)
                .map(pv -> ProductVariantResponse.builder()
                        .id(pv.getId())
                        .productId(pv.getProduct() != null ? pv.getProduct().getId() : null)
                        .listPrice(pv.getListPrice())
                        .quantity(pv.getQuantity())
                        .sku(pv.getSku())
                        .model(pv.getModel())
                        .description(pv.getDescription())
                        .warranty(pv.getWarrantyMonths() == null ? null : pv.getWarrantyMonths() + " months")
                        .barcode(pv.getBarcode())
                        .imageUrl(resolveMainImageUrl(pv))
                        .releaseAt(pv.getReleaseAt())
                        .status(pv.getStatus())
                        .createdAt(pv.getCreatedAt())
                        .build())
                .toList();
    }

    default String resolveMainImageUrl(com.ecm.server.model.ProductVariant variant) {
        if (variant == null || variant.getImages() == null) {
            return null;
        }
        return variant.getImages().stream()
                .filter(image -> image != null && image.isMain()
                        && "ACTIVE".equalsIgnoreCase(image.getStatus())
                        && image.getFile() != null)
                .map(image -> image.getFile().getPublicUrl())
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElseGet(() -> variant.getImages().stream()
                        .filter(image -> image != null && "ACTIVE".equalsIgnoreCase(image.getStatus())
                                && image.getFile() != null)
                        .map(image -> image.getFile().getPublicUrl())
                        .filter(url -> url != null && !url.isBlank())
                        .findFirst()
                        .orElse(null));
    }

    default List<java.util.UUID> mapDiscountCategories(Collection<com.ecm.server.model.DiscountCategory> categories) {
        if (categories == null) {
            return Collections.emptyList();
        }
        return categories.stream()
                .map(com.ecm.server.model.DiscountCategory::getCategory)
                .filter(java.util.Objects::nonNull)
                .map(com.ecm.server.model.Category::getId)
                .toList();
    }
}
