package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryGroupRequest {

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotBlank(message = "Group name is required")
    @Size(max = 100, message = "Group name cannot exceed 100 characters")
    private String name;

    @Builder.Default
    private Integer displayOrder = 0;
}
