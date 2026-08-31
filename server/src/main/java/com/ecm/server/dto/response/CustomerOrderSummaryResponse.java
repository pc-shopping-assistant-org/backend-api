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
public class CustomerOrderSummaryResponse {
    private UUID orderId;
    private Instant orderTime;
    private Long totalAmount;
    private Long discountAmount;
    private Long shippingFee;
    private String status;
    private String deliveryAddress;
    private String recipientName;
    private String recipientPhone;
}
