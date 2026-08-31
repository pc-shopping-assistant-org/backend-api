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
public class EmployeeDetailResponse {
    private UUID id;
    private UUID accountId;
    private UUID roleId;
    private String roleName;
    private String fullName;
    private UUID avatarFileId;
    private String email;
    private String phone;
    private String gender;
    private LocalDate birthday;
    private Long salary;
    private LocalDate joinedAt;
    private String address;
    private String status;
    private Instant createdAt;
}
