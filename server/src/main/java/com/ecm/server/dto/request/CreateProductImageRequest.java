package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductImageRequest {

    private String name;

    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    @Builder.Default
    private Boolean isMain = false;
}
