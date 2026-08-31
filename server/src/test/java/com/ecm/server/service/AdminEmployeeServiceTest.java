package com.ecm.server.service;

import com.ecm.server.dto.request.UpdateEmployeeRequest;
import com.ecm.server.dto.request.EmployeeFilterRequest;
import com.ecm.server.dto.response.EmployeeDetailResponse;
import com.ecm.server.mapper.UserMapper;
import com.ecm.server.model.Account;
import com.ecm.server.model.Employee;
import com.ecm.server.model.Role;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.FileRepository;
import com.ecm.server.repository.RoleRepository;
import com.ecm.server.service.impl.AdminEmployeeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import java.util.List;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminEmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AdminEmployeeServiceImpl service;

    @Test
    void updateEmployeeKeepsOptionalBirthdayAndAddressWhenOmitted() {
        UUID employeeId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        LocalDate originalBirthday = LocalDate.of(1990, 4, 12);

        Account account = Account.builder()
                .id(accountId)
                .email("employee@example.com")
                .phone("0901234567")
                .status("ACTIVE")
                .build();
        Employee employee = Employee.builder()
                .accountId(accountId)
                .account(account)
                .firstName("Old")
                .lastName("Name")
                .gender("MALE")
                .birthday(originalBirthday)
                .address("12 Old Street")
                .build();
        Role role = Role.builder().id(roleId).name("ROLE_EMPLOYEE").status("ACTIVE").build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toEmployeeDetail(any(Employee.class)))
                .thenReturn(EmployeeDetailResponse.builder().build());

        service.updateEmployee(employeeId, UpdateEmployeeRequest.builder()
                .roleId(roleId)
                .fullName("New Name")
                .build());

        assertEquals(originalBirthday, employee.getBirthday());
        assertEquals("12 Old Street", employee.getAddress());
    }

    @Test
    void getEmployeesNormalizesRoleAndStatusFilters() {
        when(employeeRepository.findEmployeesInitial(any(), eq("ROLE_MANAGER"), eq("LOCKED"), any(Pageable.class)))
                .thenReturn(List.of());

        service.getEmployees(EmployeeFilterRequest.builder()
                .roleName(" role_manager ")
                .status(" locked ")
                .limit(10)
                .build());

        verify(employeeRepository, times(1)).findEmployeesInitial(
                any(), eq("ROLE_MANAGER"), eq("LOCKED"), any(Pageable.class));
    }
}
