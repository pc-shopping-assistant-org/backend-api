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
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductVariantRequest {

    @NotNull(message = "List price is required")
    @Min(value = 0, message = "List price cannot be negative")
    private Long listPrice;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @Size(max = 100, message = "Model cannot exceed 100 characters")
    private String model;

    private String description;

    @Size(max = 100, message = "Warranty cannot exceed 100 characters")
    @Pattern(
            regexp = "^\\s*(?:[1-9][0-9]*\\s*(?:(?i:months?)|tháng)?\\s*)?$",
            message = "Warranty must be a positive number of months"
    )
    private String warranty;

    @Size(max = 100, message = "Barcode cannot exceed 100 characters")
    private String barcode;

    private LocalDate releaseAt;

    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status must be ACTIVE or INACTIVE; use the delete endpoint for DELETED")
    private String status;

    @Builder.Default
    private List<UUID> optionIds = new ArrayList<>();
}
