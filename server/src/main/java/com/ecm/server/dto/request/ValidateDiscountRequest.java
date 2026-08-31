package com.ecm.server.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateDiscountRequest {

    @NotBlank(message = "Discount code is required")
    private String code;

    @NotNull(message = "Order amount is required")
    @Min(value = 0, message = "Order amount cannot be negative")
    private Long orderAmount;

    private List<@Valid OrderItemValidateDto> items;
}
