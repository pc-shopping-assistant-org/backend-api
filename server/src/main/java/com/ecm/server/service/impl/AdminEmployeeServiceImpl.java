package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateEmployeeRequest;
import com.ecm.server.dto.request.EmployeeFilterRequest;
import com.ecm.server.dto.request.UpdateEmployeeRequest;
import com.ecm.server.dto.request.UpdateUserStatusRequest;
import com.ecm.server.dto.response.EmployeeDetailResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.UserMapper;
import com.ecm.server.model.Account;
import com.ecm.server.model.Employee;
import com.ecm.server.model.Role;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.RoleRepository;
import com.ecm.server.service.AdminEmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEmployeeServiceImpl implements AdminEmployeeService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DELETED = "DELETED";

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<EmployeeDetailResponse> getEmployees(EmployeeFilterRequest request) {
        // 1. Fetch limit + 1 records using cursor query to avoid count query overhead
        int limit = request.getSanitizedLimit();
        int queryLimit = limit + 1;
        UUID cursorUuid = (request.getCursor() != null && !request.getCursor().isBlank())
                ? UUID.fromString(request.getCursor())
                : null;

        List<Employee> employees = employeeRepository.findEmployeesByCursor(
                cursorUuid,
                request.getKeyword(),
                request.getRoleName(),
                request.getStatus(),
                queryLimit
        );

        // 2. Transform entity list to DTO list via MapStruct
        List<EmployeeDetailResponse> dtoList = employees.stream()
                .map(userMapper::toEmployeeDetail)
                .toList();

        // 3. Build cursor page response
        return CursorPageResponse.of(dtoList, limit, item -> item.getId().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDetailResponse getEmployeeById(UUID id) {
        // 1. Fetch employee entity by ID
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StatusCode.EMPLOYEE_NOT_FOUND));

        // 2. Map entity to detail DTO via MapStruct
        return userMapper.toEmployeeDetail(employee);
    }

    @Override
    @Transactional
    public EmployeeDetailResponse createEmployee(CreateEmployeeRequest request) {
        // 1. Validate username, email, and phone uniqueness
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(StatusCode.USERNAME_ALREADY_EXISTS);
        }
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(StatusCode.EMAIL_ALREADY_EXISTS);
        }
        if (request.getPhone() != null && employeeRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(StatusCode.PHONE_ALREADY_EXISTS);
        }

        // 2. Validate role assignment
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Role not found with id: " + request.getRoleId()));

        // 3. Persist new account entity with hashed password
        Account account = Account.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status(STATUS_ACTIVE)
                .build();
        Account savedAccount = accountRepository.save(account);

        // 4. Persist new employee profile entity
        Employee employee = Employee.builder()
                .account(savedAccount)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .gender(request.getGender())
                .birthday(request.getBirthday())
                .address(request.getAddress())
                .status(STATUS_ACTIVE)
                .build();
        Employee savedEmployee = employeeRepository.save(employee);

        // 5. Map and return created employee detail DTO
        return userMapper.toEmployeeDetail(savedEmployee);
    }

    @Override
    @Transactional
    public EmployeeDetailResponse updateEmployee(UUID id, UpdateEmployeeRequest request) {
        // 1. Retrieve existing employee entity
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StatusCode.EMPLOYEE_NOT_FOUND));

        // 2. Validate role assignment
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Role not found with id: " + request.getRoleId()));

        // 3. Verify phone uniqueness if changed
        if (request.getPhone() != null && !request.getPhone().equals(employee.getPhone())) {
            if (employeeRepository.existsByPhone(request.getPhone())) {
                throw new BusinessException(StatusCode.PHONE_ALREADY_EXISTS);
            }
            employee.setPhone(request.getPhone());
        }

        // 4. Update employee profile and account role
        employee.setFullName(request.getFullName());
        employee.setGender(request.getGender());
        employee.setBirthday(request.getBirthday());
        employee.setAddress(request.getAddress());

        Account account = employee.getAccount();
        account.setRole(role);
        accountRepository.save(account);

        Employee updatedEmployee = employeeRepository.save(employee);

        // 5. Map and return updated employee detail DTO
        return userMapper.toEmployeeDetail(updatedEmployee);
    }

    @Override
    @Transactional
    public void updateEmployeeStatus(UUID id, UpdateUserStatusRequest request) {
        // 1. Retrieve employee entity by ID
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StatusCode.EMPLOYEE_NOT_FOUND));

        // 2. Synchronize status across employee and account entities
        String newStatus = request.getStatus().toUpperCase();
        employee.setStatus(newStatus);
        employeeRepository.save(employee);

        Account account = employee.getAccount();
        if (account != null) {
            account.setStatus(newStatus);
            accountRepository.save(account);
        }

        // 3. Log status modification event
        log.info("Updated employee [{}] status to [{}] with reason: {}", id, newStatus, request.getReason());
    }
}
