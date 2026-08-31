package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.ChangePasswordRequest;
import com.ecm.server.dto.request.UpdateProfileRequest;
import com.ecm.server.dto.response.UserProfileResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.UserMapper;
import com.ecm.server.model.Account;
import com.ecm.server.model.Customer;
import com.ecm.server.model.CustomerAddress;
import com.ecm.server.model.Employee;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.repository.CustomerAddressRepository;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.FileRepository;
import com.ecm.server.service.UserProfileService;
import com.ecm.server.service.OtpService;
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

    public static final String OTP_PURPOSE_CHANGE_PASSWORD = "CHANGE_PASSWORD";

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final EmployeeRepository employeeRepository;
    private final FileRepository fileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final OtpService otpService;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID accountId) {
        // 1. Fetch account entity by account ID
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.ACCOUNT_NOT_FOUND));

        // 2. Resolve associated customer profile if present
        Optional<Customer> customerOpt = customerRepository.findByAccountId(accountId);
        if (customerOpt.isPresent()) {
            UserProfileResponse response = userMapper.toProfile(account, customerOpt.get());
            customerAddressRepository.findByCustomerAccountIdAndIsDefaultTrue(accountId)
                    .ifPresent(address -> response.setAddress(address.getAddressLine()));
            return response;
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
            String requestedEmail = normalizeOptional(request.getEmail());
            String requestedPhone = normalizeOptional(request.getPhone());
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
            userMapper.updateCustomerFromRequest(request, customer);
            if (request.getAvatarFileId() != null) {
                customer.setAvatarFileId(resolveAvatarFileId(request.getAvatarFileId()));
            }
            Customer updatedCustomer = customerRepository.save(customer);
            accountRepository.save(account);
            CustomerAddress address = customerAddressRepository.findByCustomerAccountIdAndIsDefaultTrue(accountId)
                    .orElseGet(() -> CustomerAddress.builder().customer(updatedCustomer).isDefault(true).build());
            address.setRecipientName(request.getFullName().trim());
            address.setPhone(account.getPhone());
            if (request.getAddress() != null && !request.getAddress().isBlank()) {
                address.setAddressLine(request.getAddress().trim());
            } else if (address.getAddressLine() == null) {
                throw new BusinessException(StatusCode.VALIDATION_ERROR, "Address is required for a customer profile");
            }
            customerAddressRepository.save(address);
            UserProfileResponse response = userMapper.toProfile(account, updatedCustomer);
            response.setAddress(address.getAddressLine());
            return response;
        }

        // 3. Handle employee profile update via MapStruct @MappingTarget
        Optional<Employee> employeeOpt = employeeRepository.findByAccountId(accountId);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            String requestedEmail = normalizeOptional(request.getEmail());
            String requestedPhone = normalizeOptional(request.getPhone());
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
            userMapper.updateEmployeeFromRequest(request, employee);
            if (request.getAvatarFileId() != null) {
                employee.setAvatarFileId(resolveAvatarFileId(request.getAvatarFileId()));
            }
            accountRepository.save(account);
            Employee updatedEmployee = employeeRepository.save(employee);
            return userMapper.toProfile(account, updatedEmployee);
        }

        throw new BusinessException(StatusCode.CUSTOMER_NOT_FOUND, "Profile not found for this account");
    }

    private UUID resolveAvatarFileId(UUID fileId) {
        return fileRepository.findById(fileId)
                .filter(file -> "ACTIVE".equalsIgnoreCase(file.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.IMAGE_NOT_FOUND,
                        "Referenced avatar file was not found"))
                .getId();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Override
    @Transactional
    public void changePassword(UUID accountId, ChangePasswordRequest request) {
        // 1. Fetch account by account ID
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.ACCOUNT_NOT_FOUND));

        // 2. Verify current password matches stored hash
        if (!passwordEncoder.matches(request.getOldPassword(), account.getPasswordHash())) {
            throw new BusinessException(StatusCode.INVALID_CREDENTIALS, "Current password does not match");
        }

        if (!otpService.verifyOtp(account.getEmail(), OTP_PURPOSE_CHANGE_PASSWORD, request.getOtp())) {
            throw new BusinessException(StatusCode.VALIDATION_ERROR, "Invalid or expired OTP code");
        }

        // 3. Hash and persist new password
        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
        otpService.deleteOtp(account.getEmail(), OTP_PURPOSE_CHANGE_PASSWORD);
    }

    @Override
    public void requestChangePasswordOtp(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(StatusCode.ACCOUNT_NOT_FOUND));
        otpService.generateAndSaveOtp(account.getEmail(), OTP_PURPOSE_CHANGE_PASSWORD);
    }
}
