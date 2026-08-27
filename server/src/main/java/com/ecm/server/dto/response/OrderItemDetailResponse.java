package com.ecm.server.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDetailResponse {

    private UUID id;
    private UUID productVariantId;
    private UUID productId;
    private String productName;
    private String sku;
    private String model;
    private String imageUrl;
    private Integer quantity;
    private Integer unitAmount;
    private Integer discountAmount;
    private Long totalAmount;
}
