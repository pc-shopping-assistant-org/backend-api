package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateEmployeeRequest;
import com.ecm.server.dto.request.EmployeeFilterRequest;
import com.ecm.server.dto.request.UpdateEmployeeRequest;
import com.ecm.server.dto.request.UpdateUserStatusRequest;
import com.ecm.server.dto.response.EmployeeDetailResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.UserMappingSupport;
import com.ecm.server.mapper.UserMapper;
import com.ecm.server.model.Account;
import com.ecm.server.model.Employee;
import com.ecm.server.model.Role;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.FileRepository;
import com.ecm.server.repository.RoleRepository;
import com.ecm.server.service.AdminEmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;
import java.util.Locale;
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
    private final FileRepository fileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<EmployeeDetailResponse> getEmployees(EmployeeFilterRequest request) {
        // 1. Fetch limit + 1 records using cursor query to avoid count query overhead
        int limit = request.getSanitizedLimit();
        int queryLimit = limit + 1;
        UUID cursorUuid = (request.getCursor() != null && !request.getCursor().isBlank())
                ? UUID.fromString(request.getCursor())
                : null;
        String keywordPattern = (request.getKeyword() != null && !request.getKeyword().isBlank())
                ? "%" + request.getKeyword().trim().toLowerCase() + "%"
                : null;
        String roleFilter = normalizeEnumFilter(request.getRoleName());
        String statusFilter = normalizeEnumFilter(request.getStatus());

        Pageable pageable = PageRequest.of(0, queryLimit);
        List<Employee> employees = (cursorUuid == null)
                ? employeeRepository.findEmployeesInitial(keywordPattern, roleFilter, statusFilter, pageable)
                : employeeRepository.findEmployeesAfterCursor(cursorUuid, keywordPattern, roleFilter, statusFilter, pageable);

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
        // 1. Validate account identity uniqueness
        if (accountRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BusinessException(StatusCode.EMAIL_ALREADY_EXISTS);
        }
        if (request.getPhone() != null && accountRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(StatusCode.PHONE_ALREADY_EXISTS);
        }

        // 2. Validate role assignment
        Role role = roleRepository.findById(request.getRoleId())
                .filter(candidate -> "ACTIVE".equalsIgnoreCase(candidate.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Role not found with id: " + request.getRoleId()));

        // 3. Persist new account entity with hashed password
        Account account = Account.builder()
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status(STATUS_ACTIVE)
                .build();
        Account savedAccount = accountRepository.save(account);

        // 4. Persist new employee profile entity
        Employee employee = Employee.builder()
                .firstName(UserMappingSupport.firstName(request.getFullName()))
                .lastName(UserMappingSupport.lastName(request.getFullName()))
                .gender(request.getGender().trim().toUpperCase(Locale.ROOT))
                .birthday(request.getBirthday())
                .salary(request.getSalary() == null ? 0L : request.getSalary())
                .joinedAt(request.getJoinedAt() == null ? LocalDate.now() : request.getJoinedAt())
                .address(request.getAddress())
                .avatarFileId(resolveAvatarFileId(request.getAvatarFileId()))
                .build();
        employee.setAccount(savedAccount);
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
                .filter(candidate -> "ACTIVE".equalsIgnoreCase(candidate.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Role not found with id: " + request.getRoleId()));

        // 3. Verify phone uniqueness if changed
        Account account = employee.getAccount();
        String requestedEmail = request.getEmail() == null || request.getEmail().isBlank()
                ? null : request.getEmail().trim();
        String requestedPhone = request.getPhone() == null || request.getPhone().isBlank()
                ? null : request.getPhone().trim();
        if (requestedEmail != null && !requestedEmail.equalsIgnoreCase(account.getEmail())) {
            if (accountRepository.existsByEmailIgnoreCase(requestedEmail)) {
                throw new BusinessException(StatusCode.EMAIL_ALREADY_EXISTS);
            }
            account.setEmail(requestedEmail.toLowerCase(java.util.Locale.ROOT));
        }
        if (requestedPhone != null && !requestedPhone.equals(account.getPhone())) {
            if (accountRepository.existsByPhone(requestedPhone)) {
                throw new BusinessException(StatusCode.PHONE_ALREADY_EXISTS);
            }
            account.setPhone(requestedPhone);
        }

        // 4. Update employee profile and account role
        employee.setFirstName(UserMappingSupport.firstName(request.getFullName()));
        employee.setLastName(UserMappingSupport.lastName(request.getFullName()));
        if (request.getGender() != null && !request.getGender().isBlank()) {
            employee.setGender(request.getGender().trim().toUpperCase(Locale.ROOT));
        }
        // Optional update fields follow the use-case contract: omitted values
        // keep the current profile instead of silently clearing it.
        if (request.getBirthday() != null) {
            employee.setBirthday(request.getBirthday());
        }
        if (request.getSalary() != null) {
            employee.setSalary(request.getSalary());
        }
        if (request.getJoinedAt() != null) {
            employee.setJoinedAt(request.getJoinedAt());
        }
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            employee.setAddress(request.getAddress().trim());
        }
        if (request.getAvatarFileId() != null) {
            employee.setAvatarFileId(resolveAvatarFileId(request.getAvatarFileId()));
        }

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

        // 2. Harmonize status codes between employee profile and account tables
        String rawStatus = request.getStatus().toUpperCase();
        String accountStatus = "BLOCKED".equals(rawStatus) ? "LOCKED" : rawStatus;

        Account account = employee.getAccount();
        if (account != null) {
            account.setStatus(accountStatus);
            accountRepository.save(account);

            // 3. Update Redis blocked blacklist for stateless JWT revocation
            String blockedKey = "account:blocked:" + account.getId();
            try {
                if ("LOCKED".equalsIgnoreCase(accountStatus) || "BLOCKED".equalsIgnoreCase(accountStatus) || "DELETED".equalsIgnoreCase(accountStatus)) {
                    // Account locking is durable until an explicit ACTIVE
                    // update; a fixed TTL would silently re-enable old JWTs.
                    redisTemplate.opsForValue().set(blockedKey, "BLOCKED");
                } else if ("ACTIVE".equalsIgnoreCase(accountStatus)) {
                    redisTemplate.delete(blockedKey);
                }
            } catch (Exception ex) {
                // Redis is only a revocation hint; account.status remains the
                // source of truth and the JWT filter checks it directly.
                log.warn("Could not update account block cache for employee [{}]: {}", id, ex.getMessage());
            }
        }

        // 4. Log status modification event
        log.info("Updated employee [{}] status to [{}] with reason: {}", id, accountStatus, request.getReason());
    }

    private UUID resolveAvatarFileId(UUID fileId) {
        if (fileId == null) {
            return null;
        }
        return fileRepository.findById(fileId)
                .filter(file -> "ACTIVE".equalsIgnoreCase(file.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.IMAGE_NOT_FOUND,
                        "Referenced avatar file was not found"))
                .getId();
    }

    private String normalizeEnumFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
