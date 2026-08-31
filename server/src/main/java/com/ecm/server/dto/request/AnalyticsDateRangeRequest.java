package com.ecm.server.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDateRangeRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private String period; // DAY, WEEK, MONTH

    @AssertTrue(message = "Period must be DAY, WEEK, or MONTH")
    public boolean hasValidPeriod() {
        return period == null || period.isBlank()
                || "DAY".equalsIgnoreCase(period)
                || "WEEK".equalsIgnoreCase(period)
                || "MONTH".equalsIgnoreCase(period);
    }

    @AssertTrue(message = "Start date must be before or equal to end date")
    public boolean hasValidDateRange() {
        return fromDate == null || toDate == null || !fromDate.isAfter(toDate);
    }
}
