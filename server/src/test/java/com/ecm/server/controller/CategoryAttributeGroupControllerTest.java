package com.ecm.server.controller;

import com.ecm.server.dto.request.AssignAttributeRequest;
import com.ecm.server.dto.request.CreateCategoryGroupRequest;
import com.ecm.server.dto.response.CategoryAttributeGroupResponse;
import com.ecm.server.dto.response.CategoryAttributeResponse;
import com.ecm.server.dto.response.CategorySpecsSchemaResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.CategoryAttributeGroupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryAttributeGroupControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CategoryAttributeGroupService categoryAttributeGroupService;

    @InjectMocks
    private CategoryAttributeGroupController categoryAttributeGroupController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryAttributeGroupController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getCategorySpecsSchema_shouldReturnSchema() throws Exception {
        UUID categoryId = UUID.randomUUID();
        CategorySpecsSchemaResponse schema = CategorySpecsSchemaResponse.builder()
                .categoryId(categoryId)
                .categoryName("Smartphones")
                .groups(List.of(CategorySpecsSchemaResponse.GroupSchemaItem.builder()
                        .groupId(UUID.randomUUID())
                        .groupName("Display")
                        .displayOrder(1)
                        .attributes(List.of())
                        .build()))
                .build();

        when(categoryAttributeGroupService.getCategorySpecsSchema(categoryId)).thenReturn(schema);

        mockMvc.perform(get("/api/v1/categories/{categoryId}/specs-schema", categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.categoryName").value("Smartphones"))
                .andExpect(jsonPath("$.data.groups[0].groupName").value("Display"));
    }

    @Test
    void createGroup_whenValidPayload_shouldReturnCreated() throws Exception {
        UUID categoryId = UUID.randomUUID();
        CreateCategoryGroupRequest request = CreateCategoryGroupRequest.builder()
                .categoryId(categoryId)
                .name("Camera")
                .displayOrder(2)
                .build();

        CategoryAttributeGroupResponse response = CategoryAttributeGroupResponse.builder()
                .id(UUID.randomUUID())
                .categoryId(categoryId)
                .name("Camera")
                .displayOrder(2)
                .status("ACTIVE")
                .build();

        when(categoryAttributeGroupService.createGroup(any(CreateCategoryGroupRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/category-attributes/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(20100))
                .andExpect(jsonPath("$.data.name").value("Camera"));
    }

    @Test
    void assignAttribute_whenValidPayload_shouldReturnCreated() throws Exception {
        AssignAttributeRequest request = AssignAttributeRequest.builder()
                .categoryGroupId(UUID.randomUUID())
                .attributeId(UUID.randomUUID())
                .required(true)
                .displayOrder(1)
                .build();

        CategoryAttributeResponse response = CategoryAttributeResponse.builder()
                .id(UUID.randomUUID())
                .categoryGroupId(request.getCategoryGroupId())
                .attributeId(request.getAttributeId())
                .attributeKey("main_camera")
                .attributeDisplayName("Camera Chính")
                .required(true)
                .displayOrder(1)
                .status("ACTIVE")
                .build();

        when(categoryAttributeGroupService.assignAttribute(any(AssignAttributeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/category-attributes/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attributeKey").value("main_camera"));
    }
}
