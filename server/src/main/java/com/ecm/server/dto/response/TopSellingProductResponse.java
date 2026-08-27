package com.ecm.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
