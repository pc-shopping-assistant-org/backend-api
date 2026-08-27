package com.ecm.server.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
