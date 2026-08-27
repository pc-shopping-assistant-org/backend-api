package com.ecm.server.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailResponse {

    private UUID id;
    private UUID orderId;
    private String method;
    private Instant paidAt;
    private String transactionCode;
    private String status;
    private Long amount;
    private Instant createdAt;
}
