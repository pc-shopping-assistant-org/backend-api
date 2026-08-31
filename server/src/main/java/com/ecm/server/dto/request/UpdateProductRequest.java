package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name cannot exceed 255 characters")
    private String name;

    @NotBlank(message = "SEO name is required")
    @Size(max = 255, message = "SEO name cannot exceed 255 characters")
    private String seoName;

    private UUID brandId;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @Builder.Default
    private java.util.List<UUID> supplierIds = new java.util.ArrayList<>();

    @Builder.Default
    private Map<String, Object> specifications = new HashMap<>();

    private String description;

    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status must be ACTIVE or INACTIVE; use the delete endpoint for DELETED")
    private String status;
}
