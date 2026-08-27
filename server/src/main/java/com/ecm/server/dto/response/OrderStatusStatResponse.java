package com.ecm.server.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusStatResponse {

    private String status;
    private Long count;
    private Double percentage;
}
