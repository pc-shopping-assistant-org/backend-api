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
public class OrderFilterRequest {

    private UUID cursor;
    private Integer limit;
    private UUID customerId;
    private String status;
    private Instant fromDate;
    private Instant toDate;
}
