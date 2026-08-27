package com.ecm.server.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueChartPointResponse {

    private String dateLabel;
    private Long revenue;
    private Long orderCount;
}
