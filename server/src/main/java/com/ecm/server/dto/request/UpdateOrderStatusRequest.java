package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotBlank(message = "Order status is required")
    @Pattern(
            regexp = "^(?i)(PENDING_PAYMENT|PENDING_CONFIRMATION|CONFIRMED|SHIPPING|COMPLETED|CANCELLED)$",
            message = "Status must be one of: PENDING_PAYMENT, PENDING_CONFIRMATION, CONFIRMED, SHIPPING, COMPLETED, CANCELLED"
    )
    private String status;

    private String reason;
}
