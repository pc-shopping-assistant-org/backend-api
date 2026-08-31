package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Status values supported by the product aggregate. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(
            regexp = "^(?i)(ACTIVE|INACTIVE)$",
            message = "Product status must be ACTIVE or INACTIVE; use the delete endpoint for DELETED"
    )
    private String status;

    private String reason;
}
