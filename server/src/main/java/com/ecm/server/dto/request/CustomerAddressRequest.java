package com.ecm.server.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request used to create or replace a customer's saved delivery address. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddressRequest {

    @NotBlank(message = "Recipient name is required")
    @Size(max = 100, message = "Recipient name cannot exceed 100 characters")
    private String recipientName;

    @NotBlank(message = "Recipient phone number is required")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Phone number must be a valid Vietnamese phone number")
    private String phone;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String addressLine;

    /** The first saved address is made default automatically by the service. */
    @JsonProperty("default")
    @Builder.Default
    private boolean isDefault = false;
}
