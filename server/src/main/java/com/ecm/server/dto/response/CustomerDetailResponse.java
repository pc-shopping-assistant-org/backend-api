package com.ecm.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDetailResponse {
    private UUID id;
    private UUID accountId;
    private String fullName;
    private UUID avatarFileId;
    private String email;
    private String phone;
    private String gender;
    private LocalDate birthday;
    private String address;
    @Builder.Default
    private List<CustomerAddressResponse> addresses = new ArrayList<>();
    private String status;
    private Instant createdAt;
    private long totalOrders;
    private long totalSpent;
}
