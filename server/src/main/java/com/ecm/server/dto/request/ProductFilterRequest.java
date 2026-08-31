package com.ecm.server.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.AssertTrue;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterRequest {

    private UUID cursor;

    @Builder.Default
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must not exceed 100")
    private Integer limit = 20;

    private UUID categoryId;

    private UUID brandId;

    @PositiveOrZero(message = "Minimum price cannot be negative")
    private Long minPrice;

    @PositiveOrZero(message = "Maximum price cannot be negative")
    private Long maxPrice;

    private String keyword;

    private String status;

    private String sortBy;

    private String sortDirection;

    @AssertTrue(message = "Minimum price must be less than or equal to maximum price")
    public boolean hasValidPriceRange() {
        return minPrice == null || maxPrice == null || minPrice <= maxPrice;
    }
}
