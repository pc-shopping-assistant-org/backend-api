package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignAttributeRequest {

    @NotNull(message = "Category group ID is required")
    private UUID categoryGroupId;

    @NotNull(message = "Attribute ID is required")
    private UUID attributeId;

    @Builder.Default
    private Boolean required = false;

    @Builder.Default
    private Integer displayOrder = 0;
}
