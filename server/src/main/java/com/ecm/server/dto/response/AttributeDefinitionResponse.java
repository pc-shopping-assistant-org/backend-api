package com.ecm.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeDefinitionResponse {
    private UUID id;
    private String key;
    private String displayName;
    private String dataType;
    private String unit;
    private List<String> allowedValues;
    private List<String> aliases;
    private boolean filterable;
    private boolean comparable;
    private String status;
    private Instant createdAt;
}
