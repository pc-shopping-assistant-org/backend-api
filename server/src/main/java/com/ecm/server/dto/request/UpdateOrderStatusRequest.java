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
            regexp = "^(?i)(PENDING|CONFIRM|SHIPPING|COMPLETED|CANCELLED)$",
            message = "Status must be one of: PENDING, CONFIRM, SHIPPING, COMPLETED, CANCELLED"
    )
    private String status;

    private String reason;
}
