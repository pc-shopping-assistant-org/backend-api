package com.ecm.server.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueChartDataResponse {

    private String period;
    private Long totalRevenue;
    private Long totalOrders;
    private List<RevenueChartPointResponse> dataPoints;
}
