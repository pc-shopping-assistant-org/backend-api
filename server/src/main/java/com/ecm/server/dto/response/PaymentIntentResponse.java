package com.ecm.server.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse {

    private UUID paymentId;
    private UUID orderId;
    private String clientSecret;
    private Long amount;
    private String currency;
    private String publishableKey;
}
