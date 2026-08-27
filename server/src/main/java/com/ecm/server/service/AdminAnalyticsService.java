package com.ecm.server.service;

import com.ecm.server.dto.request.AnalyticsDateRangeRequest;
import com.ecm.server.dto.response.DashboardOverviewResponse;
import com.ecm.server.dto.response.OrderStatusStatResponse;
import com.ecm.server.dto.response.RevenueChartDataResponse;
import com.ecm.server.dto.response.TopSellingProductResponse;

import java.time.LocalDate;
import java.util.List;

public interface AdminAnalyticsService {

    DashboardOverviewResponse getDashboardOverview();

    RevenueChartDataResponse getRevenueChart(AnalyticsDateRangeRequest request);

    List<TopSellingProductResponse> getTopSellingProducts(Integer limit, LocalDate fromDate, LocalDate toDate);

    List<OrderStatusStatResponse> getOrderStatusStats();
}
