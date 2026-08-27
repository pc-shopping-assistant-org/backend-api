package com.ecm.server.dto.response;

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
public class ReviewResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private UUID customerId;
    private String customerName;
    private Integer rating;
    private String comment;
    private Boolean isVerifiedPurchase;
    private String status;
    private Instant createdAt;
}
