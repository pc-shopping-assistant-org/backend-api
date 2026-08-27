package com.ecm.server.dto.request;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFilterRequest {

    private UUID cursor;
    private Integer limit;
    private UUID orderId;
    private String method;
    private String status;
    private String transactionCode;
    private Instant fromDate;
    private Instant toDate;
}
