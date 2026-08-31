package com.ecm.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {

    private UUID id;
    private String name;
    private String seoName;
    private UUID brandId;
    private String brandName;
    private UUID categoryId;
    private String categoryName;
    @Builder.Default
    private java.util.List<SupplierResponse> suppliers = new java.util.ArrayList<>();
    private Long minPrice;
    private Long maxPrice;
    private String imageUrl;

    @Builder.Default
    private Double ratingAverage = 0.0;

    @Builder.Default
    private Integer reviewCount = 0;

    private String status;
    private Instant createdAt;
}
