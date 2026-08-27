package com.ecm.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResponse {

    private UUID id;
    private String name;
    private UUID productVariantId;
    private String imageUrl;
    private boolean isMain;
    private String status;
    private Instant createdAt;
}
