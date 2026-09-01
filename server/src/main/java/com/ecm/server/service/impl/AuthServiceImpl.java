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
import com.ecm.server.model.CustomerAddress;
import com.ecm.server.model.Employee;
import com.ecm.server.model.Role;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.repository.CustomerAddressRepository;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.RoleRepository;
import com.ecm.server.service.AuthService;
import com.ecm.server.service.OtpService;
import com.ecm.server.service.google.GoogleIdentity;
import com.ecm.server.service.google.GoogleIdentityVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

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
    private static final long MIN_REVOCATION_MARKER_TTL_MS = 1L;

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final OtpService otpService;
    private final GoogleIdentityVerifier googleIdentityVerifier;
    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;

    @Override
    public void register(RegisterRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase(Locale.ROOT);
        String phone = request.getPhone() == null ? null : request.getPhone().trim();
        // Email and phone are account identity fields, not profile duplicates.
        if (accountRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(StatusCode.EMAIL_ALREADY_EXISTS);
        }
        if (accountRepository.existsByPhone(phone)) {
            throw new BusinessException(StatusCode.PHONE_ALREADY_EXISTS);
        }

        // 2. Cache registration payload in Redis
        request.setEmail(email);
        request.setPhone(phone);
        otpService.savePendingRegistration(email, request);

        // 3. Generate 6-digit OTP code and cache in Redis
        otpService.generateAndSaveOtp(email, OTP_PURPOSE_REGISTRATION);
    }

    @Override
    @Transactional
    public AuthResponse verifyRegistrationOtp(VerifyOtpRequest request) {
        // 1. Validate OTP from Redis
        boolean isValid = otpService.verifyOtp(request.getEmail(), OTP_PURPOSE_REGISTRATION, request.getOtp());
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
                .email(regData.getEmail().trim().toLowerCase())
                .phone(regData.getPhone().trim())
                .passwordHash(passwordEncoder.encode(regData.getPassword()))
                .role(customerRole)
                .status(STATUS_ACTIVE)
                .build();
        Account savedAccount = accountRepository.save(account);

        // 5. Convert DTO to Customer entity via MapStruct and persist
        Customer customer = userMapper.toCustomer(regData);
        customer.setAccount(savedAccount);
        Customer savedCustomer = customerRepository.save(customer);

        customerAddressRepository.save(CustomerAddress.builder()
                .customer(savedCustomer)
                .recipientName(regData.getFullName().trim())
                .phone(regData.getPhone().trim())
                .addressLine(regData.getAddress().trim())
                .isDefault(true)
                .build());

        // 6. Evict temporary cache keys from Redis
        otpService.deleteOtp(request.getEmail(), OTP_PURPOSE_REGISTRATION);
        otpService.deletePendingRegistration(request.getEmail());

        // 7. Generate JWT access and refresh token pair via MapStruct
        String accessToken = tokenProvider.generateAccessToken(savedAccount.getId(), savedAccount.getEmail(), customerRole.getName());
        String refreshToken = tokenProvider.generateRefreshToken(savedAccount.getId(), savedAccount.getEmail());
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
        String purpose = request.getPurpose().trim().toUpperCase(Locale.ROOT);

        // 1. Verify existence of pending registration if applicable
        if (OTP_PURPOSE_REGISTRATION.equals(purpose)) {
            RegisterRequest regData = otpService.getPendingRegistration(request.getEmail());
            if (regData == null) {
                throw new BusinessException(StatusCode.VALIDATION_ERROR, "No pending registration found for this email");
            }
        }

        // 2. Generate and store fresh OTP in Redis
        otpService.generateAndSaveOtp(request.getEmail(), purpose);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 1. Retrieve account by canonical email-or-phone identifier
        String identifier = request.getIdentifier().trim();
        Account account = accountRepository.findByLoginIdentifier(identifier)
                .orElseThrow(() -> new BusinessException(StatusCode.INVALID_CREDENTIALS));

        // 2. Validate account active state
        ensureAccountUsable(account);

        // 3. Match hashed password
        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new BusinessException(StatusCode.INVALID_CREDENTIALS);
        }

        // 4. Build user summary and generate JWT tokens
        return issueTokenPair(account);
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdentity identity = googleIdentityVerifier.verify(request.getIdToken());

        // The stable provider subject is the lookup key after the first link.
        Account account = accountRepository.findByGoogleSubject(identity.subject()).orElse(null);
        if (account == null) {
            // Email is used only to link an already registered local account.
            // This preserves the required local phone/address onboarding flow.
            account = accountRepository.findByEmailIgnoreCase(identity.email())
                    .orElseThrow(() -> new BusinessException(StatusCode.GOOGLE_ACCOUNT_NOT_LINKED));

            if (StringUtils.hasText(account.getGoogleSubject())
                    && !identity.subject().equals(account.getGoogleSubject())) {
                // Do not silently rebind a local account to another Google
                // identity when the provider subject has already been set.
                throw new BusinessException(StatusCode.INVALID_CREDENTIALS);
            }

            ensureAccountUsable(account);
            account.setGoogleSubject(identity.subject());
            account = accountRepository.save(account);
        } else {
            ensureAccountUsable(account);
        }

        return issueTokenPair(account);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // 1. Validate refresh token structure and signature
        String refreshToken = request.getRefreshToken();
        if (isTokenBlacklisted(refreshToken) || !tokenProvider.validateToken(refreshToken)
                || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(StatusCode.TOKEN_INVALID, "Invalid or expired refresh token");
        }

        UUID tokenAccountId = tokenProvider.getAccountIdFromToken(refreshToken);
        if (tokenAccountId == null || isAccountTokenRevoked(tokenAccountId, refreshToken)) {
            throw new BusinessException(StatusCode.TOKEN_INVALID, "Invalid or expired refresh token");
        }

        // 2. Extract the email/phone subject and retrieve account
        String identifier = tokenProvider.getIdentifierFromToken(refreshToken);
        Account account = accountRepository.findByLoginIdentifier(identifier)
                .orElseThrow(() -> new BusinessException(StatusCode.ACCOUNT_NOT_FOUND));

        if (STATUS_LOCKED.equalsIgnoreCase(account.getStatus()) || STATUS_BLOCKED.equalsIgnoreCase(account.getStatus())) {
            throw new BusinessException(StatusCode.ACCOUNT_LOCKED);
        }
        if (!STATUS_ACTIVE.equalsIgnoreCase(account.getStatus())) {
            throw new BusinessException(StatusCode.ACCOUNT_INACTIVE);
        }

        // 3. Re-issue fresh access and refresh token pair
        String roleName = account.getRole() != null ? account.getRole().getName() : ROLE_CUSTOMER;
        UUID employeeId = employeeRepository.findByAccountId(account.getId())
                .map(com.ecm.server.model.Employee::getAccountId)
                .orElse(null);
        String newAccessToken = tokenProvider.generateAccessToken(account.getId(), account.getEmail(), roleName, employeeId);
        String newRefreshToken = tokenProvider.generateRefreshToken(account.getId(), account.getEmail());
        UserSummaryResponse userSummary = buildUserSummary(account);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtProperties.getAccessTokenExpirationMs() / MILLIS_IN_SECOND)
                .user(userSummary)
                .build();
    }

    @Override
    public void logout(String bearerToken, String refreshToken) {
        // The access token is always carried by the authenticated request.
        if (StringUtils.hasText(bearerToken)
                && bearerToken.startsWith(JwtAuthenticationFilter.BEARER_PREFIX)) {
            String accessToken = bearerToken.substring(BEARER_PREFIX_LENGTH);
            blacklistToken(accessToken);
            revokeAccountTokens(accessToken);
        }

        // The refresh token remains optional for clients that only hold an
        // access token. The account-level revocation marker above invalidates
        // every older refresh token, while this key keeps the supplied token
        // explicitly blacklisted as well.
        if (StringUtils.hasText(refreshToken)) {
            if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
                throw new BusinessException(StatusCode.TOKEN_INVALID, "Invalid refresh token");
            }
            blacklistToken(refreshToken);
        }
    }

    private void blacklistToken(String token) {
        try {
            long remainingTimeMs = tokenProvider.getExpirationTimeMsFromToken(token);
            if (remainingTimeMs > 0) {
                String key = JwtAuthenticationFilter.BLACKLIST_TOKEN_PREFIX + token;
                redisTemplate.opsForValue().set(key, "LOGGED_OUT", Duration.ofMillis(remainingTimeMs));
            }
        } catch (Exception ex) {
            log.error("Failed to write blacklisted token to Redis during logout", ex);
            throw new BusinessException(
                    StatusCode.SERVICE_UNAVAILABLE,
                    "Session service is unavailable; logout was not completed"
            );
        }
    }

    private void revokeAccountTokens(String accessToken) {
        try {
            UUID accountId = tokenProvider.getAccountIdFromToken(accessToken);
            if (accountId == null) {
                return;
            }

            long ttlMs = Math.max(
                    MIN_REVOCATION_MARKER_TTL_MS,
                    jwtProperties.getRefreshTokenExpirationMs()
            );
            String key = JwtAuthenticationFilter.ACCOUNT_REVOKED_BEFORE_PREFIX + accountId;
            // A logout invalidates all tokens issued before this cutoff second.
            // The marker lives as long as the configured refresh-token lifetime.
            redisTemplate.opsForValue().set(
                    key,
                    Long.toString(System.currentTimeMillis() / MILLIS_IN_SECOND),
                    Duration.ofMillis(ttlMs)
            );
        } catch (Exception ex) {
            log.error("Failed to write account token revocation marker to Redis during logout", ex);
            throw new BusinessException(
                    StatusCode.SERVICE_UNAVAILABLE,
                    "Session service is unavailable; logout was not completed"
            );
        }
    }

    private boolean isTokenBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(
                    JwtAuthenticationFilter.BLACKLIST_TOKEN_PREFIX + token));
        } catch (Exception ex) {
            log.error("Redis unavailable during refresh-token blacklist check", ex);
            throw new BusinessException(
                    StatusCode.SERVICE_UNAVAILABLE,
                    "Session service is unavailable; please try again later"
            );
        }
    }

    private boolean isAccountTokenRevoked(UUID accountId, String token) {
        String key = JwtAuthenticationFilter.ACCOUNT_REVOKED_BEFORE_PREFIX + accountId;
        try {
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                return false;
            }

            String revokedBeforeValue = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(revokedBeforeValue)) {
                return true;
            }

            long revokedBefore = Long.parseLong(revokedBeforeValue);
            // JWT NumericDate values have second precision. Use a strict
            // cutoff so a fresh login in the same second as logout is not
            // accidentally rejected by the marker.
            long issuedAtSecond = tokenProvider.getIssuedAtTimeMsFromToken(token) / MILLIS_IN_SECOND;
            return issuedAtSecond < revokedBefore;
        } catch (Exception ex) {
            log.error("Redis unavailable during refresh-token account revocation check", ex);
            throw new BusinessException(
                    StatusCode.SERVICE_UNAVAILABLE,
                    "Session service is unavailable; please try again later"
            );
        }
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        // 1. Resolve either canonical email or phone through accounts.
        Account account = accountRepository.findByLoginIdentifier(request.getLoginIdentifier())
                .orElseThrow(() -> new BusinessException(StatusCode.ACCOUNT_NOT_FOUND));

        // 2. OTP delivery is email-based today, regardless of which account
        // identity the customer used to request the reset.
        otpService.generateAndSaveOtp(account.getEmail(), OTP_PURPOSE_FORGOT_PASSWORD);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 1. Resolve the account from either email or phone, then verify the
        // OTP against its canonical email delivery key.
        Account account = accountRepository.findByLoginIdentifier(request.getLoginIdentifier())
                .orElseThrow(() -> new BusinessException(StatusCode.ACCOUNT_NOT_FOUND));
        boolean isValid = otpService.verifyOtp(account.getEmail(), OTP_PURPOSE_FORGOT_PASSWORD, request.getOtp());
        if (!isValid) {
            throw new BusinessException(StatusCode.VALIDATION_ERROR, "Invalid or expired OTP code");
        }

        // 2. Hash and update new password
        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);

        // 3. Evict OTP from Redis
        otpService.deleteOtp(account.getEmail(), OTP_PURPOSE_FORGOT_PASSWORD);
    }

    private void ensureAccountUsable(Account account) {
        if (STATUS_LOCKED.equalsIgnoreCase(account.getStatus())
                || STATUS_BLOCKED.equalsIgnoreCase(account.getStatus())) {
            throw new BusinessException(StatusCode.ACCOUNT_LOCKED);
        }
        if (STATUS_DELETED.equalsIgnoreCase(account.getStatus())
                || !STATUS_ACTIVE.equalsIgnoreCase(account.getStatus())) {
            throw new BusinessException(StatusCode.ACCOUNT_INACTIVE);
        }
    }

    private AuthResponse issueTokenPair(Account account) {
        String roleName = account.getRole() != null ? account.getRole().getName() : ROLE_CUSTOMER;
        UUID employeeId = employeeRepository.findByAccountId(account.getId())
                .map(Employee::getAccountId)
                .orElse(null);
        String accessToken = tokenProvider.generateAccessToken(account.getId(), account.getEmail(), roleName, employeeId);
        String refreshToken = tokenProvider.generateRefreshToken(account.getId(), account.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getAccessTokenExpirationMs() / MILLIS_IN_SECOND)
                .user(buildUserSummary(account))
                .build();
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
                        .role(account.getRole() != null ? account.getRole().getName() : ROLE_CUSTOMER)
                        .email(account.getEmail())
                        .phone(account.getPhone())
                        .build());
    }
}
