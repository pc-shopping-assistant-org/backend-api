package com.ecm.server.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewFilterRequest {

    private UUID cursor;
    private Integer limit;
    private Integer rating;
    private UUID productId;
    private String status;
    private String keyword;
}
