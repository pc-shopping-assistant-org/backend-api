package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.ChangePasswordRequest;
import com.ecm.server.dto.request.UpdateProfileRequest;
import com.ecm.server.dto.response.UserProfileResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.UserMapper;
import com.ecm.server.model.Account;
import com.ecm.server.model.Customer;
import com.ecm.server.model.Employee;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID accountId) {
        // 1. Fetch account entity by account ID
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.ACCOUNT_NOT_FOUND));

        // 2. Resolve associated customer profile if present
        Optional<Customer> customerOpt = customerRepository.findByAccountId(accountId);
        if (customerOpt.isPresent()) {
            return userMapper.toProfile(account, customerOpt.get());
        }

        // 3. Resolve associated employee profile if present
        Optional<Employee> employeeOpt = employeeRepository.findByAccountId(accountId);
        if (employeeOpt.isPresent()) {
            return userMapper.toProfile(account, employeeOpt.get());
        }

        throw new BusinessException(StatusCode.CUSTOMER_NOT_FOUND, "Profile not found for this account");
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID accountId, UpdateProfileRequest request) {
        // 1. Fetch account entity
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.ACCOUNT_NOT_FOUND));

        // 2. Handle customer profile update via MapStruct @MappingTarget
        Optional<Customer> customerOpt = customerRepository.findByAccountId(accountId);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            if (request.getPhone() != null && !request.getPhone().equals(customer.getPhone())) {
                if (customerRepository.existsByPhone(request.getPhone())) {
                    throw new BusinessException(StatusCode.PHONE_ALREADY_EXISTS);
                }
            }
            userMapper.updateCustomerFromRequest(request, customer);
            Customer updatedCustomer = customerRepository.save(customer);
            return userMapper.toProfile(account, updatedCustomer);
        }

        // 3. Handle employee profile update via MapStruct @MappingTarget
        Optional<Employee> employeeOpt = employeeRepository.findByAccountId(accountId);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            if (request.getPhone() != null && !request.getPhone().equals(employee.getPhone())) {
                if (employeeRepository.existsByPhone(request.getPhone())) {
                    throw new BusinessException(StatusCode.PHONE_ALREADY_EXISTS);
                }
            }
            userMapper.updateEmployeeFromRequest(request, employee);
            Employee updatedEmployee = employeeRepository.save(employee);
            return userMapper.toProfile(account, updatedEmployee);
        }

        throw new BusinessException(StatusCode.CUSTOMER_NOT_FOUND, "Profile not found for this account");
    }

    @Override
    @Transactional
    public void changePassword(UUID accountId, ChangePasswordRequest request) {
        // 1. Fetch account by account ID
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.ACCOUNT_NOT_FOUND));

        // 2. Verify current password matches stored hash
        if (!passwordEncoder.matches(request.getOldPassword(), account.getPassword())) {
            throw new BusinessException(StatusCode.INVALID_CREDENTIALS, "Current password does not match");
        }

        // 3. Hash and persist new password
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
    }
}
