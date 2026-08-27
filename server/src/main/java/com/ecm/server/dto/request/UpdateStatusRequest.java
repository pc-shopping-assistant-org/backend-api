package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(
            regexp = "^(?i)(ACTIVE|INACTIVE|EXPIRED|DISABLED|LOCKED|BLOCKED|DELETED)$",
            message = "Invalid status value"
    )
    private String status;

    private String reason;
}
