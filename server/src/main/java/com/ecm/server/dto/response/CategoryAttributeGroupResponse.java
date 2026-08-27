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
public class CategoryAttributeGroupResponse {
    private UUID id;
    private UUID categoryId;
    private String name;
    private int displayOrder;
    private String status;
    private Instant createdAt;
}
