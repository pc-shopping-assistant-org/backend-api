package com.ecm.server.controller;

import com.ecm.server.dto.request.CreateCategoryRequest;
import com.ecm.server.dto.response.CategoryResponse;
import com.ecm.server.dto.response.CategoryTreeResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.CategoryService;
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
class CategoryControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getCategoryTree_shouldReturnRootCategories() throws Exception {
        UUID categoryId = UUID.randomUUID();
        CategoryTreeResponse treeResponse = CategoryTreeResponse.builder()
                .id(categoryId)
                .name("Electronics")
                .seoName("electronics")
                .status("ACTIVE")
                .children(List.of())
                .build();

        when(categoryService.getCategoryTree()).thenReturn(List.of(treeResponse));

        mockMvc.perform(get("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].name").value("Electronics"));
    }

    @Test
    void createCategory_whenValidPayload_shouldReturnCreated() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Smartphones")
                .seoName("smartphones")
                .build();

        CategoryResponse response = CategoryResponse.builder()
                .id(UUID.randomUUID())
                .name("Smartphones")
                .seoName("smartphones")
                .status("ACTIVE")
                .build();

        when(categoryService.createCategory(any(CreateCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("CREATED"))
                .andExpect(jsonPath("$.data.name").value("Smartphones"));
    }
}
