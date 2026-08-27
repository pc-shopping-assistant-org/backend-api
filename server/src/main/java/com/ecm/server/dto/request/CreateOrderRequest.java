package com.ecm.server.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotEmpty(message = "Order items list cannot be empty")
    @Valid
    private List<OrderItemRequest> items;

    private String discountCode;

    @NotBlank(message = "Recipient name is required")
    private String recipientName;

    @NotBlank(message = "Recipient phone number is required")
    private String recipientPhone;

    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;

    private String note;

    @NotBlank(message = "Payment method is required")
    @Pattern(
        regexp = "^(?i)(COD|VNPAY|MOMO|SEPAY|BANK_TRANSFER)$",
        message = "Payment method must be one of: COD, VNPAY, MOMO, SEPAY, BANK_TRANSFER"
    )
    private String paymentMethod;
}
