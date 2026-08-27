package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentIntentRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotBlank(message = "Payment method is required")
    @Pattern(
            regexp = "^(?i)(STRIPE|CREDIT_CARD|COD|VNPAY|MOMO|SEPAY|BANK_TRANSFER)$",
            message = "Payment method must be one of: STRIPE, CREDIT_CARD, COD, VNPAY, MOMO, SEPAY, BANK_TRANSFER"
    )
    private String paymentMethod;
}
