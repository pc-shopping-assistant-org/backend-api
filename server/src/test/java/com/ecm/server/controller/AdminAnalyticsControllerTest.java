package com.ecm.server.controller;

import com.ecm.server.dto.response.DashboardOverviewResponse;
import com.ecm.server.dto.response.OrderStatusStatResponse;
import com.ecm.server.dto.response.RevenueChartDataResponse;
import com.ecm.server.dto.response.RevenueChartPointResponse;
import com.ecm.server.dto.response.TopSellingProductResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.AdminAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminAnalyticsService adminAnalyticsService;

    @InjectMocks
    private AdminAnalyticsController adminAnalyticsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminAnalyticsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getDashboardOverview_shouldReturnOverview() throws Exception {
        DashboardOverviewResponse response = DashboardOverviewResponse.builder()
                .totalRevenue(50000000L)
                .totalOrders(100L)
                .completedOrders(80L)
                .cancelledOrders(5L)
                .totalCustomers(150L)
                .newCustomersThisMonth(15L)
                .revenueGrowthRate(12.5)
                .build();

        when(adminAnalyticsService.getDashboardOverview()).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalRevenue").value(50000000))
                .andExpect(jsonPath("$.data.completedOrders").value(80));
    }

    @Test
    void getRevenueChart_shouldReturnChartData() throws Exception {
        RevenueChartDataResponse response = RevenueChartDataResponse.builder()
                .period("DAY")
                .totalRevenue(10000000L)
                .totalOrders(5L)
                .dataPoints(List.of(RevenueChartPointResponse.builder()
                        .dateLabel("2026-08-27")
                        .revenue(10000000L)
                        .orderCount(5L)
                        .build()))
                .build();

        when(adminAnalyticsService.getRevenueChart(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/analytics/revenue-chart")
                        .param("period", "DAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.period").value("DAY"))
                .andExpect(jsonPath("$.data.dataPoints[0].revenue").value(10000000));
    }

    @Test
    void getTopSellingProducts_shouldReturnList() throws Exception {
        TopSellingProductResponse item = TopSellingProductResponse.builder()
                .productId(UUID.randomUUID())
                .productName("iPhone 16 Pro Max")
                .totalQuantitySold(15L)
                .totalRevenue(500000000L)
                .build();

        when(adminAnalyticsService.getTopSellingProducts(any(), any(), any()))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/admin/analytics/top-selling")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].productName").value("iPhone 16 Pro Max"))
                .andExpect(jsonPath("$.data[0].totalQuantitySold").value(15));
    }

    @Test
    void getOrderStatusStats_shouldReturnStatusStats() throws Exception {
        OrderStatusStatResponse stat = OrderStatusStatResponse.builder()
                .status("COMPLETED")
                .count(80L)
                .percentage(80.0)
                .build();

        when(adminAnalyticsService.getOrderStatusStats()).thenReturn(List.of(stat));

        mockMvc.perform(get("/api/v1/admin/analytics/order-status-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data[0].count").value(80));
    }
}
