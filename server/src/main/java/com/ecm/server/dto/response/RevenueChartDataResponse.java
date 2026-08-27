package com.ecm.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
