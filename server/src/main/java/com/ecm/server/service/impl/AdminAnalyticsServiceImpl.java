package com.ecm.server.service.impl;

import com.ecm.server.dto.request.AnalyticsDateRangeRequest;
import com.ecm.server.dto.response.*;
import com.ecm.server.model.Order;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.repository.OrderItemRepository;
import com.ecm.server.repository.OrderRepository;
import com.ecm.server.repository.ProductImageRepository;
import com.ecm.server.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final int DEFAULT_TOP_LIMIT = 5;
    public static final int MAX_TOP_LIMIT = 50;

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewResponse getDashboardOverview() {
        // 1. Fetch total revenue and order counts by status
        Long totalRevenue = orderRepository.sumTotalRevenue();
        long totalOrders = orderRepository.count();
        long completedOrders = orderRepository.countByStatus(STATUS_COMPLETED);
        long cancelledOrders = orderRepository.countByStatus(STATUS_CANCELLED);
        long totalCustomers = customerRepository.count();

        // 2. Calculate current and previous month customer registration and revenue growth
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        Instant startOfThisMonth = now.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfThisMonth = now.plusMonths(1).withDayOfMonth(1)
                .atStartOfDay(ZoneOffset.UTC).toInstant();

        Long newCustomersThisMonth = customerRepository.countNewCustomersBetween(startOfThisMonth, endOfThisMonth);

        YearMonth currentYearMonth = YearMonth.from(now);
        YearMonth prevYearMonth = currentYearMonth.minusMonths(1);

        Instant startOfCurrent = currentYearMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfCurrent = currentYearMonth.atEndOfMonth().atTime(23, 59, 59).toInstant(ZoneOffset.UTC);

        Instant startOfPrev = prevYearMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfPrev = prevYearMonth.atEndOfMonth().atTime(23, 59, 59).toInstant(ZoneOffset.UTC);

        Long currentMonthRevenue = orderRepository.sumTotalRevenueBetween(startOfCurrent, endOfCurrent);
        Long prevMonthRevenue = orderRepository.sumTotalRevenueBetween(startOfPrev, endOfPrev);

        double growthRate = 0.0;
        if (prevMonthRevenue != null && prevMonthRevenue > 0) {
            growthRate = ((double) (currentMonthRevenue - prevMonthRevenue) / prevMonthRevenue) * 100.0;
            growthRate = Math.round(growthRate * 10.0) / 10.0;
        }

        // 3. Assemble and return DashboardOverviewResponse
        return DashboardOverviewResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : 0L)
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .totalCustomers(totalCustomers)
                .newCustomersThisMonth(newCustomersThisMonth != null ? newCustomersThisMonth : 0L)
                .revenueGrowthRate(growthRate)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueChartDataResponse getRevenueChart(AnalyticsDateRangeRequest request) {
        // 1. Resolve date range and period type
        String period = (request != null && request.getPeriod() != null) ? request.getPeriod().toUpperCase() : "DAY";
        LocalDate toDate = (request != null && request.getToDate() != null) ? request.getToDate() : LocalDate.now(ZoneOffset.UTC);
        LocalDate fromDate;

        if (request != null && request.getFromDate() != null) {
            fromDate = request.getFromDate();
        } else {
            fromDate = switch (period) {
                case "WEEK" -> toDate.minusWeeks(12);
                case "MONTH" -> toDate.minusMonths(11);
                default -> toDate.minusDays(29);
            };
        }
        if (fromDate.isAfter(toDate)) {
            throw new com.ecm.server.exception.BusinessException(
                    com.ecm.server.common.StatusCode.VALIDATION_ERROR,
                    "Analytics start date must be before or equal to end date");
        }

        Instant fromInstant = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = toDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);

        // 2. Initialize contiguous chart date slots
        Map<String, long[]> bucketMap = new LinkedHashMap<>();
        if ("MONTH".equals(period)) {
            YearMonth startYm = YearMonth.from(fromDate);
            YearMonth endYm = YearMonth.from(toDate);
            YearMonth curr = startYm;
            while (!curr.isAfter(endYm)) {
                bucketMap.put(curr.format(MONTH_FORMATTER), new long[]{0L, 0L});
                curr = curr.plusMonths(1);
            }
        } else if ("WEEK".equals(period)) {
            LocalDate curr = fromDate;
            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            while (!curr.isAfter(toDate)) {
                String label = curr.getYear() + "-W" + String.format("%02d", curr.get(weekFields.weekOfWeekBasedYear()));
                bucketMap.putIfAbsent(label, new long[]{0L, 0L});
                curr = curr.plusWeeks(1);
            }
        } else {
            LocalDate curr = fromDate;
            while (!curr.isAfter(toDate)) {
                bucketMap.put(curr.format(DAY_FORMATTER), new long[]{0L, 0L});
                curr = curr.plusDays(1);
            }
        }

        // 3. Query completed orders and aggregate into chart buckets
        List<Order> orders = orderRepository.findCompletedOrdersBetween(fromInstant, toInstant);
        long totalRevenue = 0L;
        long totalOrders = orders.size();

        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        for (Order order : orders) {
            LocalDate orderLocalDate = LocalDate.ofInstant(order.getOrderTime(), ZoneOffset.UTC);
            String label = switch (period) {
                case "MONTH" -> YearMonth.from(orderLocalDate).format(MONTH_FORMATTER);
                case "WEEK" ->
                        orderLocalDate.getYear() + "-W" + String.format("%02d", orderLocalDate.get(weekFields.weekOfWeekBasedYear()));
                default -> orderLocalDate.format(DAY_FORMATTER);
            };

            long amount = order.getTotalAmount() != null ? order.getTotalAmount() : 0L;
            totalRevenue += amount;

            long[] stats = bucketMap.computeIfAbsent(label, k -> new long[]{0L, 0L});
            stats[0] += amount; // Revenue
            stats[1] += 1;      // Order count
        }

        // 4. Map buckets to point responses
        List<RevenueChartPointResponse> dataPoints = new ArrayList<>();
        bucketMap.forEach((label, stats) -> dataPoints.add(RevenueChartPointResponse.builder()
                .dateLabel(label)
                .revenue(stats[0])
                .orderCount(stats[1])
                .build()));

        return RevenueChartDataResponse.builder()
                .period(period)
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .dataPoints(dataPoints)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopSellingProductResponse> getTopSellingProducts(Integer limit, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new com.ecm.server.exception.BusinessException(
                    com.ecm.server.common.StatusCode.VALIDATION_ERROR,
                    "Analytics start date must be before or equal to end date");
        }
        // 1. Resolve query bounds and pagination
        int topLimit = (limit != null && limit > 0) ? Math.min(limit, MAX_TOP_LIMIT) : DEFAULT_TOP_LIMIT;
        Instant fromInstant = (fromDate != null) ? fromDate.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant = (toDate != null) ? toDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC) : null;

        // 2. Query aggregate top sold items
        List<Object[]> rows = orderItemRepository.findTopSellingProducts(fromInstant, toInstant, PageRequest.of(0, topLimit));

        // 3. Map aggregate projections to TopSellingProductResponse
        List<TopSellingProductResponse> results = new ArrayList<>();
        for (Object[] row : rows) {
            String imageUrl = productImageRepository.findActiveForProduct((UUID) row[0]).stream()
                    .map(image -> image.getFile() == null ? null : image.getFile().getPublicUrl())
                    .filter(url -> url != null && !url.isBlank())
                    .findFirst().orElse(null);
            results.add(TopSellingProductResponse.builder()
                    .productId((UUID) row[0])
                    .productName((String) row[1])
                    .imageUrl(imageUrl)
                    .totalQuantitySold(((Number) row[3]).longValue())
                    .totalRevenue(((Number) row[4]).longValue())
                    .build());
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusStatResponse> getOrderStatusStats() {
        // 1. Retrieve order count breakdown by status
        List<Object[]> rows = orderRepository.countOrdersByStatus();
        long totalOrders = 0L;
        for (Object[] row : rows) {
            totalOrders += ((Number) row[1]).longValue();
        }

        // 2. Compute percentages for each status
        List<OrderStatusStatResponse> stats = new ArrayList<>();
        for (Object[] row : rows) {
            String status = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double pct = (totalOrders > 0) ? (count * 100.0) / totalOrders : 0.0;
            pct = Math.round(pct * 10.0) / 10.0;

            stats.add(OrderStatusStatResponse.builder()
                    .status(status)
                    .count(count)
                    .percentage(pct)
                    .build());
        }

        // 3. Return status breakdown response
        return stats;
    }
}
