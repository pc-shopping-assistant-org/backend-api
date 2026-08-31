package com.ecm.server.dto.request;

import jakarta.validation.constraints.NotNull;
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

    /** Existing media registry entry referenced by the image row. */
    @NotNull(message = "File ID is required")
    private java.util.UUID fileId;

    @Builder.Default
    private Boolean isMain = false;

}
