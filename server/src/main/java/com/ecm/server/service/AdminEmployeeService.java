package com.ecm.server.service;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.dto.request.CreateEmployeeRequest;
import com.ecm.server.dto.request.EmployeeFilterRequest;
import com.ecm.server.dto.request.UpdateEmployeeRequest;
import com.ecm.server.dto.request.UpdateUserStatusRequest;
import com.ecm.server.dto.response.EmployeeDetailResponse;

import java.util.UUID;

public interface AdminEmployeeService {

    CursorPageResponse<EmployeeDetailResponse> getEmployees(EmployeeFilterRequest request);

    EmployeeDetailResponse getEmployeeById(UUID id);

    EmployeeDetailResponse createEmployee(CreateEmployeeRequest request);

    EmployeeDetailResponse updateEmployee(UUID id, UpdateEmployeeRequest request);

    void updateEmployeeStatus(UUID id, UpdateUserStatusRequest request);
}
