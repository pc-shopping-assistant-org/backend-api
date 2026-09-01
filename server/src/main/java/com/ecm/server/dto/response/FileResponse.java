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
public class FileResponse {

    private UUID id;
    private String storageProvider;
    private String originalName;
    private String mimeType;
    private long sizeBytes;
    private String publicUrl;
    private String status;
    private Instant createdAt;
}
