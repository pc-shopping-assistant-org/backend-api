package com.ecm.server.controller;

import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.model.ShippingMethod;
import com.ecm.server.repository.ShippingMethodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShippingMethodControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShippingMethodRepository shippingMethodRepository;

    @InjectMocks
    private ShippingMethodController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getActiveShippingMethodsReturnsSortedCatalogWithFees() throws Exception {
        when(shippingMethodRepository.findAllByStatusIgnoreCaseOrderByFeeAscCodeAsc(eq("ACTIVE")))
                .thenReturn(List.of(
                        ShippingMethod.builder()
                                .id(UUID.randomUUID())
                                .code("EXPRESS")
                                .name("Express")
                                .fee(30_000L)
                                .status("ACTIVE")
                                .build()
                ));

        mockMvc.perform(get("/api/v1/shipping-methods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].code").value("EXPRESS"))
                .andExpect(jsonPath("$.data[0].fee").value(30_000));
    }
}
