package com.ecm.server.dto.request;

import lombok.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFilterRequest {

    private UUID cursor;
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must not exceed 100")
    private Integer limit;
    private UUID customerId;

    /** Customer-facing order/invoice search (UUID or INV-XXXXXXXX prefix). */
    @Size(max = 100, message = "Order search keyword must not exceed 100 characters")
    private String keyword;

    private String status;
    private Instant fromDate;
    private Instant toDate;
}
