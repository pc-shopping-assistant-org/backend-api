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
    @Mapping(target = "imageUrl", source = "productVariant.imageUrl")
    @Mapping(target = "totalAmount", expression = "java(((long) entity.getQuantity() * entity.getUnitAmount()) - entity.getDiscountAmount())")
    OrderItemDetailResponse toDetailResponse(OrderItem entity);

    List<OrderItemDetailResponse> toDetailResponseList(Collection<OrderItem> entities);
}
