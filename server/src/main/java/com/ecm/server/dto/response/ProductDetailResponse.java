package com.ecm.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {

    private UUID id;
    private String name;
    private String seoName;
    private BrandResponse brand;
    private CategoryResponse category;
    @Builder.Default
    private List<SupplierResponse> suppliers = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> specifications = new HashMap<>();

    private String description;
    private String imageUrl;

    @Builder.Default
    private Double ratingAverage = 0.0;

    @Builder.Default
    private Integer reviewCount = 0;

    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<ProductVariantResponse> variants = new ArrayList<>();
}
