package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(
            regexp = "^(ACTIVE|LOCKED|BLOCKED|DELETED)$",
            message = "Status must be one of: ACTIVE, LOCKED, BLOCKED, DELETED"
    )
    private String status;

    private String reason;
}
