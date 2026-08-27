package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private String transactionCode;

    private String note;
}
