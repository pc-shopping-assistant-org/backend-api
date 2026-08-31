package com.ecm.server.dto.request;

import lombok.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountFilterRequest {

    private UUID cursor;
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must not exceed 100")
    private Integer limit;
    private String keyword;
    private String discountType;
    private String applicationScope;
    private String status;
}
