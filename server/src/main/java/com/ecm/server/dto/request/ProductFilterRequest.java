package com.ecm.server.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterRequest {

    private UUID cursor;

    @Builder.Default
    private Integer limit = 20;

    private UUID categoryId;

    private UUID brandId;

    private Integer minPrice;

    private Integer maxPrice;

    private String keyword;

    private String status;

    private String sortBy;

    private String sortDirection;
}
