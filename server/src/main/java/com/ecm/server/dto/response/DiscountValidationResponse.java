package com.ecm.server.dto.response;

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
public class DiscountValidationResponse {

    private Boolean isValid;
    private UUID discountId;
    private String code;
    private String title;
    private Integer discountAmount;
    private Long finalAmount;
    private String message;
}
