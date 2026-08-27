package com.ecm.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAttributeResponse {
    private UUID id;
    private UUID categoryGroupId;
    private UUID attributeId;
    private String attributeKey;
    private String attributeDisplayName;
    private boolean required;
    private int displayOrder;
    private String status;
}
