package com.ecm.server.dto.response;

import lombok.*;

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
