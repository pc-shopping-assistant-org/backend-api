package com.ecm.server.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
