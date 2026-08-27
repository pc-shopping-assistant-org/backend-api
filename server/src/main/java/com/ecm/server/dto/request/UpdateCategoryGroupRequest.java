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
public class UpdateCategoryGroupRequest {

    @NotBlank(message = "Group name is required")
    @Size(max = 100, message = "Group name cannot exceed 100 characters")
    private String name;

    @Builder.Default
    private Integer displayOrder = 0;

    @Pattern(regexp = "^(ACTIVE|INACTIVE|DELETED)$", message = "Status must be ACTIVE, INACTIVE, or DELETED")
    private String status;
}
