package com.ecm.server.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountFilterRequest {

    private UUID cursor;
    private Integer limit;
    private String keyword;
    private String type;
    private String scope;
    private String status;
}
