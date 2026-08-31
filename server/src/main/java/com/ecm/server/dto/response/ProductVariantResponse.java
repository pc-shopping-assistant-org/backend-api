package com.ecm.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {

    private UUID id;
    private UUID productId;
    private String sku;
    private Long listPrice;

    private Integer quantity;
    private String model;
    private String description;
    private String warranty;
    private String barcode;
    private String imageUrl;
    private LocalDate releaseAt;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<OptionResponse> options = new ArrayList<>();

    @Builder.Default
    private List<ProductImageResponse> images = new ArrayList<>();
}
