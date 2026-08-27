package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name cannot exceed 255 characters")
    private String name;

    @NotBlank(message = "SEO name is required")
    @Size(max = 255, message = "SEO name cannot exceed 255 characters")
    private String seoName;

    private UUID brandId;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    private UUID supplierId;

    @Builder.Default
    private Map<String, Object> specifications = new HashMap<>();

    private String description;

    private String imageUrl;

    @Builder.Default
    private List<CreateProductVariantRequest> variants = new ArrayList<>();
}
