package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.response.ReviewResponse;
import com.ecm.server.model.ProductReview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface ProductReviewMapper {

    @Mapping(target = "productId", source = "orderItem.productVariant.product.id")
    @Mapping(target = "productName", source = "orderItem.productVariant.product.name")
    @Mapping(target = "customerId", source = "orderItem.order.customer.accountId")
    @Mapping(target = "customerName", expression = "java(UserMappingSupport.fullName(entity.getOrderItem().getOrder().getCustomer().getFirstName(), entity.getOrderItem().getOrder().getCustomer().getLastName()))")
    @Mapping(target = "isVerifiedPurchase", ignore = true)
    ReviewResponse toResponse(ProductReview entity);

    List<ReviewResponse> toResponseList(List<ProductReview> entities);
}
