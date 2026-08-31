package com.ecm.server.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotEmpty(message = "Order items list cannot be empty")
    private List<@Valid OrderItemRequest> items;

    private String discountCode;

    private String recipientName;

    private String recipientPhone;

    private String deliveryAddress;

    /** Optional saved-address reference; fields above may be omitted when set. */
    private UUID customerAddressId;

    /** Normalized shipping_methods.code; STANDARD is used when omitted. */
    private String shippingMethodCode;

    private String note;

    @NotBlank(message = "Payment method is required")
    @Pattern(
            regexp = "^(?i)(COD|STRIPE_CARD|BANK_TRANSFER)$",
            message = "Payment method must be one of: COD, STRIPE_CARD, BANK_TRANSFER"
    )
    private String paymentMethod;
}
