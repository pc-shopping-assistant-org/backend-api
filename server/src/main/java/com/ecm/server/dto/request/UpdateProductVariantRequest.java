package com.ecm.server.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductVariantRequest {

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    private Integer price;

    @NotNull(message = "Sale price is required")
    @Min(value = 0, message = "Sale price cannot be negative")
    private Integer priceSale;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @Size(max = 100, message = "Model cannot exceed 100 characters")
    private String model;

    @Pattern(regexp = "^(DENY|CONTINUE|BACKORDER)$", message = "Inventory policy must be DENY, CONTINUE, or BACKORDER")
    private String inventoryPolicy;

    @Builder.Default
    private Map<String, Object> specifications = new HashMap<>();

    private String description;

    @Size(max = 100, message = "Warranty cannot exceed 100 characters")
    private String warranty;

    @Size(max = 100, message = "Barcode cannot exceed 100 characters")
    private String barcode;

    private String imageUrl;

    private LocalDate releaseAt;

    @Pattern(regexp = "^(ACTIVE|INACTIVE|DELETED)$", message = "Status must be ACTIVE, INACTIVE, or DELETED")
    private String status;

    @Builder.Default
    private List<UUID> optionIds = new ArrayList<>();
}
