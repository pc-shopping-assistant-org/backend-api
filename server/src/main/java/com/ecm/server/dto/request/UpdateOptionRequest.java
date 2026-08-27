package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOptionRequest {

    @NotBlank(message = "Option type is required")
    @Size(max = 50, message = "Option type cannot exceed 50 characters")
    private String type;

    @NotBlank(message = "Option name is required")
    @Size(max = 100, message = "Option name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Option value is required")
    private String value;

    @Pattern(regexp = "^(ACTIVE|INACTIVE|DELETED)$", message = "Status must be ACTIVE, INACTIVE, or DELETED")
    private String status;
}
