package com.ecm.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDetailResponse {
    private UUID id;
    private UUID accountId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private LocalDate birthday;
    private String address;
    private String status;
    private Instant createdAt;
    private long totalOrders;
    private long totalSpent;
}
