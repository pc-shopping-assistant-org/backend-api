package com.ecm.server.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDiscountRequest {

    /** Null for an automatic promotion; non-null for a voucher. */
    @Size(max = 50, message = "Discount code cannot exceed 50 characters")
    private String code;

    @NotBlank(message = "Discount title is required")
    private String title;

    @NotBlank(message = "Discount type is required")
    @Pattern(regexp = "^(?i)(PERCENT|FIXED)$", message = "Discount type must be either 'PERCENT' or 'FIXED'")
    private String discountType;

    @NotNull(message = "Discount value is required")
    @Min(value = 1, message = "Discount value must be greater than 0")
    private Integer value;

    @NotNull(message = "Start time is required")
    private Instant startAt;

    @NotNull(message = "End time is required")
    private Instant endAt;

    @NotBlank(message = "Scope is required")
    @Pattern(regexp = "^(?i)(ORDER|ALL_ITEMS|CATEGORY|VARIANT)$", message = "Scope must be ORDER, ALL_ITEMS, CATEGORY, or VARIANT")
    private String applicationScope;

    @Min(value = 0, message = "Minimum order amount cannot be negative")
    @Builder.Default
    private Long minOrderAmount = 0L;

    private String description;

    private List<UUID> appliedVariantIds;

    private List<UUID> appliedCategoryIds;

    @AssertTrue(message = "End time must be after start time")
    public boolean hasValidDateRange() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }

    @AssertTrue(message = "PERCENT value must be between 1 and 100; FIXED value must be greater than 0")
    public boolean hasValidTypeValue() {
        if (discountType == null || value == null) {
            return true;
        }
        return "PERCENT".equalsIgnoreCase(discountType)
                ? value > 0 && value <= 100
                : "FIXED".equalsIgnoreCase(discountType) && value > 0;
    }
}
