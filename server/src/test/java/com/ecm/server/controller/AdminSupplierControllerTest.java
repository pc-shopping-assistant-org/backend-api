package com.ecm.server.controller;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.CreateSupplierRequest;
import com.ecm.server.dto.request.SupplierFilterRequest;
import com.ecm.server.dto.response.SupplierResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.SupplierService;
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
class AdminSupplierControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SupplierService supplierService;

    @InjectMocks
    private AdminSupplierController adminSupplierController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminSupplierController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getSuppliers_shouldReturnCursorPageResponse() throws Exception {
        UUID supplierId = UUID.randomUUID();
        SupplierResponse supplier = SupplierResponse.builder()
                .id(supplierId)
                .name("Supplier Alpha")
                .email("alpha@supplier.com")
                .status("ACTIVE")
                .build();

        CursorPageResponse<SupplierResponse> pageResponse = CursorPageResponse.of(
                List.of(supplier),
                10,
                item -> item.getId().toString()
        );

        when(supplierService.getSuppliers(any(SupplierFilterRequest.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/suppliers")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].name").value("Supplier Alpha"));
    }

    @Test
    void createSupplier_whenValidPayload_shouldReturnCreated() throws Exception {
        CreateSupplierRequest request = CreateSupplierRequest.builder()
                .name("Supplier Beta")
                .email("beta@supplier.com")
                .phone("0988776655")
                .address("123 Tech Park")
                .build();

        SupplierResponse response = SupplierResponse.builder()
                .id(UUID.randomUUID())
                .name("Supplier Beta")
                .email("beta@supplier.com")
                .phone("0988776655")
                .address("123 Tech Park")
                .status("ACTIVE")
                .build();

        when(supplierService.createSupplier(any(CreateSupplierRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(20100))
                .andExpect(jsonPath("$.data.name").value("Supplier Beta"));
    }
}
