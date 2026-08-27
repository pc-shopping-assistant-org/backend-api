package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.config.security.JwtAuthenticationFilter;
import com.ecm.server.config.security.JwtProperties;
import com.ecm.server.config.security.JwtTokenProvider;
import com.ecm.server.dto.request.*;
import com.ecm.server.dto.response.AuthResponse;
import com.ecm.server.dto.response.UserSummaryResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.UserMapper;
import com.ecm.server.model.Account;
import com.ecm.server.model.Customer;
import com.ecm.server.model.Employee;
import com.ecm.server.model.Role;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.RoleRepository;
import com.ecm.server.service.AuthService;
import com.ecm.server.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_LOCKED = "LOCKED";
    public static final String STATUS_BLOCKED = "BLOCKED";
    public static final String STATUS_DELETED = "DELETED";
    public static final String OTP_PURPOSE_REGISTRATION = "REGISTRATION";
    public static final String OTP_PURPOSE_FORGOT_PASSWORD = "FORGOT_PASSWORD";
    private static final int BEARER_PREFIX_LENGTH = 7;
    private static final int MILLIS_IN_SECOND = 1000;

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final OtpService otpService;
    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;

    @Override
    public void register(RegisterRequest request) {
        // 1. Verify username, email, and phone uniqueness
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(StatusCode.USERNAME_ALREADY_EXISTS);
        }
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(StatusCode.EMAIL_ALREADY_EXISTS);
        }
        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(StatusCode.PHONE_ALREADY_EXISTS);
        }

        // 2. Cache registration payload in Redis
        otpService.savePendingRegistration(request.getEmail(), request);

        // 3. Generate 6-digit OTP code and cache in Redis
        otpService.generateAndSaveOtp(request.getEmail(), OTP_PURPOSE_REGISTRATION);
    }

    @Override
    @Transactional
    public AuthResponse verifyRegistrationOtp(VerifyOtpRequest request) {
        // 1. Validate OTP from Redis
        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getPurpose(), request.getOtp());
        if (!isValid) {
            throw new BusinessException(StatusCode.VALIDATION_ERROR, "Invalid or expired OTP code");
        }

        // 2. Retrieve cached registration payload
        RegisterRequest regData = otpService.getPendingRegistration(request.getEmail());
        if (regData == null) {
            throw new BusinessException(StatusCode.VALIDATION_ERROR, "Registration session has expired, please register again");
        }

        // 3. Fetch or initialize default CUSTOMER role
        Role customerRole = roleRepository.findByName(ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(ROLE_CUSTOMER).status(STATUS_ACTIVE).build()));

        // 4. Persist new account entity
        Account account = Account.builder()
                .username(regData.getUsername())
                .password(passwordEncoder.encode(regData.getPassword()))
                .role(customerRole)
                .status(STATUS_ACTIVE)
                .build();
        Account savedAccount = accountRepository.save(account);

        // 5. Convert DTO to Customer entity via MapStruct and persist
        Customer customer = userMapper.toCustomer(regData);
        customer.setAccount(savedAccount);
        Customer savedCustomer = customerRepository.save(customer);

        // 6. Evict temporary cache keys from Redis
        otpService.deleteOtp(request.getEmail(), request.getPurpose());
        otpService.deletePendingRegistration(request.getEmail());

        // 7. Generate JWT access and refresh token pair via MapStruct
        String accessToken = tokenProvider.generateAccessToken(savedAccount.getId(), savedAccount.getUsername(), customerRole.getName());
        String refreshToken = tokenProvider.generateRefreshToken(savedAccount.getId(), savedAccount.getUsername());
        UserSummaryResponse userSummary = userMapper.toSummary(savedAccount, savedCustomer);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getAccessTokenExpirationMs() / MILLIS_IN_SECOND)
                .user(userSummary)
                .build();
    }

    @Override
    public void resendOtp(ResendOtpRequest request) {
        // 1. Verify existence of pending registration if applicable
        if (OTP_PURPOSE_REGISTRATION.equalsIgnoreCase(request.getPurpose())) {
            RegisterRequest regData = otpService.getPendingRegistration(request.getEmail());
            if (regData == null) {
                throw new BusinessException(StatusCode.VALIDATION_ERROR, "No pending registration found for this email");
            }
        }

        // 2. Generate and store fresh OTP in Redis
        otpService.generateAndSaveOtp(request.getEmail(), request.getPurpose());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 1. Retrieve account by username
        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(StatusCode.INVALID_CREDENTIALS));

        // 2. Validate account active state
        if (STATUS_LOCKED.equalsIgnoreCase(account.getStatus()) || STATUS_BLOCKED.equalsIgnoreCase(account.getStatus())) {
            throw new BusinessException(StatusCode.ACCOUNT_LOCKED);
        }
        if (STATUS_DELETED.equalsIgnoreCase(account.getStatus()) || !STATUS_ACTIVE.equalsIgnoreCase(account.getStatus())) {
            throw new BusinessException(StatusCode.ACCOUNT_INACTIVE);
        }

        // 3. Match hashed password
        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new BusinessException(StatusCode.INVALID_CREDENTIALS);
        }

        // 4. Build user summary response via MapStruct
        String roleName = account.getRole() != null ? account.getRole().getName() : ROLE_CUSTOMER;
        UserSummaryResponse userSummary = buildUserSummary(account);

        // 5. Generate JWT tokens
        String accessToken = tokenProvider.generateAccessToken(account.getId(), account.getUsername(), roleName);
        String refreshToken = tokenProvider.generateRefreshToken(account.getId(), account.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getAccessTokenExpirationMs() / MILLIS_IN_SECOND)
                .user(userSummary)
                .build();
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // 1. Validate refresh token structure and signature
        String refreshToken = request.getRefreshToken();
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(StatusCode.TOKEN_INVALID, "Invalid or expired refresh token");
        }

        // 2. Extract username and retrieve account
        String username = tokenProvider.getUsernameFromToken(refreshToken);
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(StatusCode.ACCOUNT_NOT_FOUND));

        if (!STATUS_ACTIVE.equalsIgnoreCase(account.getStatus())) {
            throw new BusinessException(StatusCode.ACCOUNT_INACTIVE);
        }

        // 3. Re-issue fresh access and refresh token pair
        String roleName = account.getRole() != null ? account.getRole().getName() : ROLE_CUSTOMER;
        String newAccessToken = tokenProvider.generateAccessToken(account.getId(), account.getUsername(), roleName);
        String newRefreshToken = tokenProvider.generateRefreshToken(account.getId(), account.getUsername());
        UserSummaryResponse userSummary = buildUserSummary(account);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtProperties.getAccessTokenExpirationMs() / MILLIS_IN_SECOND)
                .user(userSummary)
                .build();
    }

    @Override
    public void logout(String bearerToken) {
        // 1. Extract raw JWT from Authorization header
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(BEARER_PREFIX_LENGTH);
            // 2. Compute remaining expiration and place token into Redis Blacklist
            long remainingTimeMs = tokenProvider.getExpirationTimeMsFromToken(token);
            if (remainingTimeMs > 0) {
                String key = JwtAuthenticationFilter.BLACKLIST_TOKEN_PREFIX + token;
                redisTemplate.opsForValue().set(key, "LOGGED_OUT", Duration.ofMillis(remainingTimeMs));
            }
        }
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        // 1. Check if email exists in Customer or Employee records
        boolean exists = customerRepository.existsByEmail(request.getEmail()) || employeeRepository.existsByEmail(request.getEmail());
        if (!exists) {
            throw new BusinessException(StatusCode.CUSTOMER_NOT_FOUND, "Email not found in system");
        }

        // 2. Generate and store OTP in Redis with FORGOT_PASSWORD purpose
        otpService.generateAndSaveOtp(request.getEmail(), OTP_PURPOSE_FORGOT_PASSWORD);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 1. Validate OTP from Redis
        boolean isValid = otpService.verifyOtp(request.getEmail(), OTP_PURPOSE_FORGOT_PASSWORD, request.getOtp());
        if (!isValid) {
            throw new BusinessException(StatusCode.VALIDATION_ERROR, "Invalid or expired OTP code");
        }

        // 2. Lookup corresponding Account via Customer or Employee email
        Optional<Customer> customerOpt = customerRepository.findByEmail(request.getEmail());
        Account account;
        if (customerOpt.isPresent()) {
            account = customerOpt.get().getAccount();
        } else {
            Employee employee = employeeRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BusinessException(StatusCode.ACCOUNT_NOT_FOUND));
            account = employee.getAccount();
        }

        // 3. Hash and update new password
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);

        // 4. Evict OTP from Redis
        otpService.deleteOtp(request.getEmail(), OTP_PURPOSE_FORGOT_PASSWORD);
    }

    private UserSummaryResponse buildUserSummary(Account account) {
        Optional<Customer> customerOpt = customerRepository.findByAccountId(account.getId());
        if (customerOpt.isPresent()) {
            return userMapper.toSummary(account, customerOpt.get());
        }

        Optional<Employee> employeeOpt = employeeRepository.findByAccountId(account.getId());
        return employeeOpt.map(employee -> userMapper.toSummary(account, employee))
                .orElseGet(() -> UserSummaryResponse.builder()
                        .accountId(account.getId())
                        .username(account.getUsername())
                        .role(account.getRole() != null ? account.getRole().getName() : ROLE_CUSTOMER)
                        .build());
    }
}
