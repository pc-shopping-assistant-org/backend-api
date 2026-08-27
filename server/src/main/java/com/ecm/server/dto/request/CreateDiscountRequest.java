package com.ecm.server.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDiscountRequest {

    @NotBlank(message = "Discount code is required")
    private String code;

    @NotBlank(message = "Discount title is required")
    private String title;

    @NotBlank(message = "Discount type is required")
    @Pattern(regexp = "^(?i)(PERCENT|FIXED)$", message = "Discount type must be either 'PERCENT' or 'FIXED'")
    private String type;

    @NotNull(message = "Discount value is required")
    @Min(value = 1, message = "Discount value must be greater than 0")
    private Integer value;

    @NotNull(message = "Start time is required")
    private Instant startAt;

    @NotNull(message = "End time is required")
    private Instant endAt;

    @NotBlank(message = "Scope is required")
    @Pattern(regexp = "^(?i)(ALL|PRODUCT|CATEGORY|ORDER)$", message = "Scope must be 'ALL', 'PRODUCT', 'CATEGORY', or 'ORDER'")
    private String scope;

    @Min(value = 0, message = "Minimum order amount cannot be negative")
    @Builder.Default
    private Long minOrderAmount = 0L;

    private String description;

    private List<UUID> appliedVariantIds;
}
