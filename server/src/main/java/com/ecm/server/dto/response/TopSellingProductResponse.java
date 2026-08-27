package com.ecm.server.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopSellingProductResponse {

    private UUID productId;
    private String productName;
    private String imageUrl;
    private Long totalQuantitySold;
    private Long totalRevenue;
}
