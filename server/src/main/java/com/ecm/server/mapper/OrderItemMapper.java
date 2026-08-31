package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.response.OrderItemDetailResponse;
import com.ecm.server.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collection;
import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface OrderItemMapper {

    @Mapping(target = "productVariantId", source = "productVariant.id")
    @Mapping(target = "productId", source = "productVariant.product.id")
    @Mapping(target = "productName", source = "productVariant.product.name")
    @Mapping(target = "sku", source = "productVariant.sku")
    @Mapping(target = "model", source = "productVariant.model")
    @Mapping(target = "imageUrl", expression = "java(resolveImageUrl(entity))")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "itemDiscount", source = "itemDiscount")
    @Mapping(target = "itemGross", expression = "java(entity.getUnitPrice() * entity.getQuantity())")
    @Mapping(target = "itemNet", expression = "java((entity.getUnitPrice() * entity.getQuantity()) - entity.getItemDiscount())")
    @Mapping(target = "totalAmount", expression = "java((entity.getUnitPrice() * entity.getQuantity()) - entity.getItemDiscount())")
    OrderItemDetailResponse toDetailResponse(OrderItem entity);

    List<OrderItemDetailResponse> toDetailResponseList(Collection<OrderItem> entities);

    default String resolveImageUrl(OrderItem entity) {
        if (entity == null || entity.getProductVariant() == null || entity.getProductVariant().getImages() == null) {
            return null;
        }
        return entity.getProductVariant().getImages().stream()
                .filter(image -> image.isMain() && "ACTIVE".equalsIgnoreCase(image.getStatus()))
                .findFirst()
                .or(() -> entity.getProductVariant().getImages().stream()
                        .filter(image -> "ACTIVE".equalsIgnoreCase(image.getStatus())).findFirst())
                .map(image -> image.getFile() == null ? null : image.getFile().getPublicUrl())
                .orElse(null);
    }
}
