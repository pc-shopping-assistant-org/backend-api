package com.ecm.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddressResponse {
    private UUID id;
    private String recipientName;
    private String phone;
    private String addressLine;
    private boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;
}
