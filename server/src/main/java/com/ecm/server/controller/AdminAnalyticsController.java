package com.ecm.server.controller;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.AnalyticsDateRangeRequest;
import com.ecm.server.dto.response.DashboardOverviewResponse;
import com.ecm.server.dto.response.OrderStatusStatResponse;
import com.ecm.server.dto.response.RevenueChartDataResponse;
import com.ecm.server.dto.response.TopSellingProductResponse;
import com.ecm.server.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE', 'ROLE_MANAGER')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getDashboardOverview() {
        DashboardOverviewResponse response = adminAnalyticsService.getDashboardOverview();
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<ApiResponse<RevenueChartDataResponse>> getRevenueChart(
            @ModelAttribute AnalyticsDateRangeRequest request
    ) {
        RevenueChartDataResponse response = adminAnalyticsService.getRevenueChart(request);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/top-selling")
    public ResponseEntity<ApiResponse<List<TopSellingProductResponse>>> getTopSellingProducts(
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        List<TopSellingProductResponse> response = adminAnalyticsService.getTopSellingProducts(limit, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }

    @GetMapping("/order-status-stats")
    public ResponseEntity<ApiResponse<List<OrderStatusStatResponse>>> getOrderStatusStats() {
        List<OrderStatusStatResponse> response = adminAnalyticsService.getOrderStatusStats();
        return ResponseEntity.ok(ApiResponse.success(StatusCode.SUCCESS, response));
    }
}
