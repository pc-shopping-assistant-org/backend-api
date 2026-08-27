package com.ecm.server.controller;

import com.ecm.server.dto.response.RoleResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.RoleService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminRoleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private AdminRoleController adminRoleController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminRoleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllRoles_shouldReturnListOfRoles() throws Exception {
        UUID roleId = UUID.randomUUID();
        RoleResponse roleResponse = RoleResponse.builder()
                .id(roleId)
                .name("ROLE_ADMIN")
                .status("ACTIVE")
                .build();

        when(roleService.getAllRoles()).thenReturn(List.of(roleResponse));

        mockMvc.perform(get("/api/v1/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("ROLE_ADMIN"));
    }
}
