package com.ecm.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
