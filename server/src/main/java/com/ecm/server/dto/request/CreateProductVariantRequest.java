package com.ecm.server.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductVariantRequest {

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    private Integer price;

    @NotNull(message = "Sale price is required")
    @Min(value = 0, message = "Sale price cannot be negative")
    private Integer priceSale;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Builder.Default
    private Integer quantity = 0;

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU cannot exceed 100 characters")
    private String sku;

    @Size(max = 100, message = "Model cannot exceed 100 characters")
    private String model;

    @Pattern(regexp = "^(DENY|CONTINUE|BACKORDER)$", message = "Inventory policy must be DENY, CONTINUE, or BACKORDER")
    @Builder.Default
    private String inventoryPolicy = "DENY";

    @Builder.Default
    private Map<String, Object> specifications = new HashMap<>();

    private String description;

    @Size(max = 100, message = "Warranty cannot exceed 100 characters")
    private String warranty;

    @Size(max = 100, message = "Barcode cannot exceed 100 characters")
    private String barcode;

    private String imageUrl;

    private LocalDate releaseAt;

    @Builder.Default
    private List<UUID> optionIds = new ArrayList<>();

    @Builder.Default
    private List<CreateProductImageRequest> images = new ArrayList<>();
}
