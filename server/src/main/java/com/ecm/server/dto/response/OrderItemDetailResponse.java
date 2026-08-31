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
    private Long unitPrice;
    private Long itemDiscount;
    private Long itemGross;
    private Long itemNet;
    private Long totalAmount;
}
