package com.ecm.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySpecsSchemaResponse {

    private UUID categoryId;
    private String categoryName;

    @Builder.Default
    private List<GroupSchemaItem> groups = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupSchemaItem {
        private UUID groupId;
        private String groupName;
        private int displayOrder;

        @Builder.Default
        private List<AttributeSchemaItem> attributes = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributeSchemaItem {
        private UUID assignmentId;
        private UUID attributeId;
        private String key;
        private String displayName;
        private String dataType;
        private String unit;
        private List<String> allowedValues;
        private boolean required;
        private int displayOrder;
        private boolean filterable;
        private boolean comparable;
    }
}
