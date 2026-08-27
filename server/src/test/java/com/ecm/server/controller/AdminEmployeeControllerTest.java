package com.ecm.server.controller;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.CreateEmployeeRequest;
import com.ecm.server.dto.request.EmployeeFilterRequest;
import com.ecm.server.dto.response.EmployeeDetailResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.AdminEmployeeService;
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
class AdminEmployeeControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminEmployeeService adminEmployeeService;

    @InjectMocks
    private AdminEmployeeController adminEmployeeController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminEmployeeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getEmployees_shouldReturnCursorPageResponse() throws Exception {
        UUID employeeId = UUID.randomUUID();
        EmployeeDetailResponse detail = EmployeeDetailResponse.builder()
                .id(employeeId)
                .username("emp1")
                .roleName("ROLE_EMPLOYEE")
                .fullName("Employee One")
                .email("emp1@example.com")
                .status("ACTIVE")
                .build();

        CursorPageResponse<EmployeeDetailResponse> pageResponse = CursorPageResponse.of(
                List.of(detail),
                10,
                item -> item.getId().toString()
        );

        when(adminEmployeeService.getEmployees(any(EmployeeFilterRequest.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/employees")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].username").value("emp1"));
    }

    @Test
    void createEmployee_whenValidPayload_shouldReturnCreated() throws Exception {
        UUID roleId = UUID.randomUUID();
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .username("emp_new")
                .password("Password123")
                .roleId(roleId)
                .fullName("New Employee")
                .email("emp_new@example.com")
                .phone("0912345678")
                .build();

        EmployeeDetailResponse createdDetail = EmployeeDetailResponse.builder()
                .id(UUID.randomUUID())
                .username("emp_new")
                .roleId(roleId)
                .roleName("ROLE_EMPLOYEE")
                .fullName("New Employee")
                .email("emp_new@example.com")
                .status("ACTIVE")
                .build();

        when(adminEmployeeService.createEmployee(any(CreateEmployeeRequest.class))).thenReturn(createdDetail);

        mockMvc.perform(post("/api/v1/admin/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(20100))
                .andExpect(jsonPath("$.data.username").value("emp_new"));
    }
}
