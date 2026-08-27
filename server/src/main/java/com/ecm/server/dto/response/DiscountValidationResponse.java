package com.ecm.server.dto.response;

import lombok.*;

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
