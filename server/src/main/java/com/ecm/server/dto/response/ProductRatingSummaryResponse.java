package com.ecm.server.dto.response;

import lombok.*;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingSummaryResponse {

    private UUID productId;
    private Double averageRating;
    private Long totalReviews;
    private Map<Integer, Long> ratingDistribution;
}
