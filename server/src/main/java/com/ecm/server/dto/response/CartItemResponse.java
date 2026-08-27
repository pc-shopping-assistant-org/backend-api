package com.ecm.server.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private UUID productVariantId;
    private UUID productId;
    private String productName;
    private String sku;
    private String model;
    private String imageUrl;
    private Integer price;
    private Integer priceSale;
    private Integer quantity;
    private Long subtotal;
    private Integer stockQuantity;
}
