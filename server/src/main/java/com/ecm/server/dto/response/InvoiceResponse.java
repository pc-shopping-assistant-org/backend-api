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
public class InvoiceResponse {

    private String invoiceId;
    private UUID orderId;
    private Instant issuedAt;
    private String customerName;
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;

    @Builder.Default
    private List<OrderItemDetailResponse> items = Collections.emptyList();

    private Long subtotalAmount;
    private Integer discountAmount;
    private Integer shipAmount;
    private Long totalAmount;
    private String paymentMethod;
    private String paymentStatus;
}
