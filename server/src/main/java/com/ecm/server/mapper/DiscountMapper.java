package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.request.CreateDiscountRequest;
import com.ecm.server.dto.request.UpdateDiscountRequest;
import com.ecm.server.dto.response.DiscountDetailResponse;
import com.ecm.server.dto.response.DiscountSummaryResponse;
import com.ecm.server.dto.response.ProductVariantResponse;
import com.ecm.server.model.Discount;
import com.ecm.server.model.DiscountProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper(config = GlobalMapperConfig.class, uses = {ProductVariantMapper.class})
public interface DiscountMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "discountProductVariants", ignore = true)
    Discount toEntity(CreateDiscountRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "discountProductVariants", ignore = true)
    void updateEntityFromRequest(UpdateDiscountRequest request, @MappingTarget Discount entity);

    DiscountSummaryResponse toSummaryResponse(Discount entity);

    List<DiscountSummaryResponse> toSummaryResponseList(List<Discount> entities);

    @Mapping(target = "appliedVariants", source = "discountProductVariants", qualifiedByName = "mapDiscountVariants")
    DiscountDetailResponse toDetailResponse(Discount entity);

    @Named("mapDiscountVariants")
    default List<ProductVariantResponse> mapDiscountVariants(Collection<DiscountProductVariant> dpvs) {
        if (dpvs == null) {
            return Collections.emptyList();
        }
        return dpvs.stream()
                .filter(dpv -> dpv.getProductVariant() != null && !"DELETED".equalsIgnoreCase(dpv.getStatus()))
                .map(dpv -> {
                    var pv = dpv.getProductVariant();
                    return ProductVariantResponse.builder()
                            .id(pv.getId())
                            .productId(pv.getProduct() != null ? pv.getProduct().getId() : null)
                            .price(pv.getPrice())
                            .priceSale(pv.getPriceSale())
                            .quantity(pv.getQuantity())
                            .sku(pv.getSku())
                            .model(pv.getModel())
                            .inventoryPolicy(pv.getInventoryPolicy())
                            .specifications(pv.getSpecifications())
                            .description(pv.getDescription())
                            .warranty(pv.getWarranty())
                            .barcode(pv.getBarcode())
                            .imageUrl(pv.getImageUrl())
                            .releaseAt(pv.getReleaseAt())
                            .status(pv.getStatus())
                            .createdAt(pv.getCreatedAt())
                            .build();
                })
                .toList();
    }
}
