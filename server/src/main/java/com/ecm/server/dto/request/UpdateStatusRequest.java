package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
