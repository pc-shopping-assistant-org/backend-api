package com.ecm.server.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountSummaryResponse {

    private UUID id;
    private String code;
    private String title;
    private String type;
    private Integer value;
    private Instant startAt;
    private Instant endAt;
    private String scope;
    private Long minOrderAmount;
    private String description;
    private String status;
    private Instant createdAt;
}
