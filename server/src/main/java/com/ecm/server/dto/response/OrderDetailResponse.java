package com.ecm.server.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {

    private UUID id;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;
    private String note;
    private Instant orderTime;
    private String status;
    private Long subtotalAmount;
    private Integer discountAmount;
    private Integer shipAmount;
    private Long totalAmount;
    private PaymentSummaryResponse payment;

    @Builder.Default
    private List<OrderItemDetailResponse> items = Collections.emptyList();

    private Instant deliveredAt;
    private Instant createdAt;
}
