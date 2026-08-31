package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Status values supported by the product-review aggregate. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(
            regexp = "^(?i)(ACTIVE|INACTIVE|DELETED)$",
            message = "Review status must be ACTIVE, INACTIVE, or DELETED"
    )
    private String status;

    private String reason;
}
