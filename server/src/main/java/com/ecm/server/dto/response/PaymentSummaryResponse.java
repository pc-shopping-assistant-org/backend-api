package com.ecm.server.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryResponse {

    private UUID id;
    private String paymentMethodCode;
    private Long amount;
    private Instant paidAt;
    private String providerTransactionCode;
    private String status;
}
