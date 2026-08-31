package com.ecm.server.dto.request;

import lombok.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFilterRequest {

    private UUID cursor;
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must not exceed 100")
    private Integer limit;
    private UUID orderId;
    /** Customer name, email, or phone search text. */
    private String keyword;
    private String paymentMethodCode;
    private String status;
    private String providerTransactionCode;
    private Instant fromDate;
    private Instant toDate;
}
