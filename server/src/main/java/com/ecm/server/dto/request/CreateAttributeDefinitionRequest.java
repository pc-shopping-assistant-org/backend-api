package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAttributeDefinitionRequest {

    @NotBlank(message = "Attribute key is required")
    @Size(max = 100, message = "Attribute key cannot exceed 100 characters")
    private String key;

    @NotBlank(message = "Display name is required")
    @Size(max = 255, message = "Display name cannot exceed 255 characters")
    private String displayName;

    @NotBlank(message = "Data type is required")
    @Pattern(regexp = "^(NUMBER|STRING|ENUM|BOOLEAN)$", message = "Data type must be NUMBER, STRING, ENUM, or BOOLEAN")
    private String dataType;

    @Size(max = 50, message = "Unit cannot exceed 50 characters")
    private String unit;

    private List<String> allowedValues;

    private List<String> aliases;

    @Builder.Default
    private Boolean filterable = false;

    @Builder.Default
    private Boolean comparable = false;
}
