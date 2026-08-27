package com.ecm.server.dto.request;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFilterRequest {

    private UUID cursor;
    private Integer limit;
    private UUID customerId;
    private String status;
    private Instant fromDate;
    private Instant toDate;
}
