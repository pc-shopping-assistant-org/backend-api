package com.ecm.server.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse {

    private Long totalRevenue;
    private Long totalOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private Long totalCustomers;
    private Long newCustomersThisMonth;
    private Double revenueGrowthRate;
}
