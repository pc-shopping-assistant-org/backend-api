package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentStatusRequest {

    @NotBlank(message = "Payment status is required")
    @Pattern(
            regexp = "^(?i)(PENDING|PAID|FAILED)$",
            message = "Status must be one of: PENDING, PAID, FAILED"
    )
    private String status;

    private String providerTransactionCode;

    private String note;
}
