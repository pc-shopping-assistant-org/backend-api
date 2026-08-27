package com.ecm.server.controller;

import com.ecm.server.dto.request.CreateBrandRequest;
import com.ecm.server.dto.response.BrandResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.BrandService;
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
class BrandControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BrandService brandService;

    @InjectMocks
    private BrandController brandController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(brandController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllBrands_shouldReturnListOfBrands() throws Exception {
        UUID brandId = UUID.randomUUID();
        BrandResponse brand = BrandResponse.builder()
                .id(brandId)
                .name("Apple")
                .description("Premium technology brand")
                .status("ACTIVE")
                .build();

        when(brandService.getAllBrands()).thenReturn(List.of(brand));

        mockMvc.perform(get("/api/v1/brands")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Apple"));
    }

    @Test
    void createBrand_whenValidPayload_shouldReturnCreated() throws Exception {
        CreateBrandRequest request = CreateBrandRequest.builder()
                .name("Samsung")
                .description("Global electronics leader")
                .build();

        BrandResponse response = BrandResponse.builder()
                .id(UUID.randomUUID())
                .name("Samsung")
                .description("Global electronics leader")
                .status("ACTIVE")
                .build();

        when(brandService.createBrand(any(CreateBrandRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(20100))
                .andExpect(jsonPath("$.data.name").value("Samsung"));
    }
}
