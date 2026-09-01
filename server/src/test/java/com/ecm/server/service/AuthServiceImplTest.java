package com.ecm.server.service;

import com.ecm.server.config.security.JwtAuthenticationFilter;
import com.ecm.server.config.security.JwtProperties;
import com.ecm.server.config.security.JwtTokenProvider;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.GoogleLoginRequest;
import com.ecm.server.mapper.UserMapper;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.model.Account;
import com.ecm.server.model.Role;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.CustomerAddressRepository;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.RoleRepository;
import com.ecm.server.service.impl.AuthServiceImpl;
import com.ecm.server.service.google.GoogleIdentity;
import com.ecm.server.service.google.GoogleIdentityVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CustomerAddressRepository customerAddressRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private OtpService otpService;
    @Mock
    private GoogleIdentityVerifier googleIdentityVerifier;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void logout_blacklistsUnexpiredBearerTokenForItsRemainingLifetime() {
        when(tokenProvider.getExpirationTimeMsFromToken("access-token")).thenReturn(30_000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.logout("Bearer access-token", null);

        verify(valueOperations).set(
                JwtAuthenticationFilter.BLACKLIST_TOKEN_PREFIX + "access-token",
                "LOGGED_OUT",
                Duration.ofMillis(30_000L)
        );
    }

    @Test
    void logout_withRefreshToken_blacklistsBothTokens() {
        when(tokenProvider.getExpirationTimeMsFromToken("access-token")).thenReturn(30_000L);
        when(tokenProvider.getExpirationTimeMsFromToken("refresh-token")).thenReturn(120_000L);
        when(tokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(tokenProvider.isRefreshToken("refresh-token")).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.logout("Bearer access-token", "refresh-token");

        verify(valueOperations).set(
                JwtAuthenticationFilter.BLACKLIST_TOKEN_PREFIX + "access-token",
                "LOGGED_OUT",
                Duration.ofMillis(30_000L)
        );
        verify(valueOperations).set(
                JwtAuthenticationFilter.BLACKLIST_TOKEN_PREFIX + "refresh-token",
                "LOGGED_OUT",
                Duration.ofMillis(120_000L)
        );
    }

    @Test
    void logout_revokesAllPreviouslyIssuedAccountTokens() {
        var accountId = java.util.UUID.randomUUID();
        when(tokenProvider.getExpirationTimeMsFromToken("access-token")).thenReturn(30_000L);
        when(tokenProvider.getAccountIdFromToken("access-token")).thenReturn(accountId);
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(604_800_000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.logout("Bearer access-token", null);

        verify(valueOperations).set(
                eq(JwtAuthenticationFilter.ACCOUNT_REVOKED_BEFORE_PREFIX + accountId),
                anyString(),
                eq(Duration.ofMillis(604_800_000L))
        );
    }

    @Test
    void logout_failsClosedWhenRedisCannotPersistRevocation() {
        when(tokenProvider.getExpirationTimeMsFromToken("access-token")).thenReturn(30_000L);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.logout("Bearer access-token", null)
        );

        assertEquals(com.ecm.server.common.StatusCode.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void refreshToken_failsClosedWhenRedisBlacklistCannotBeRead() {
        when(redisTemplate.hasKey(JwtAuthenticationFilter.BLACKLIST_TOKEN_PREFIX + "refresh-token"))
                .thenThrow(new IllegalStateException("redis down"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.refreshToken(
                        com.ecm.server.dto.request.RefreshTokenRequest.builder()
                                .refreshToken("refresh-token")
                                .build()
                )
        );

        assertEquals(com.ecm.server.common.StatusCode.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void refreshToken_rejectsTokenIssuedBeforeAccountLogout() {
        var accountId = java.util.UUID.randomUUID();
        String revocationKey = JwtAuthenticationFilter.ACCOUNT_REVOKED_BEFORE_PREFIX + accountId;
        when(redisTemplate.hasKey(JwtAuthenticationFilter.BLACKLIST_TOKEN_PREFIX + "refresh-token"))
                .thenReturn(false);
        when(tokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(tokenProvider.isRefreshToken("refresh-token")).thenReturn(true);
        when(tokenProvider.getAccountIdFromToken("refresh-token")).thenReturn(accountId);
        when(redisTemplate.hasKey(revocationKey)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(revocationKey)).thenReturn("2000");
        when(tokenProvider.getIssuedAtTimeMsFromToken("refresh-token")).thenReturn(1000L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.refreshToken(
                        com.ecm.server.dto.request.RefreshTokenRequest.builder()
                                .refreshToken("refresh-token")
                                .build()
                )
        );

        assertEquals(com.ecm.server.common.StatusCode.TOKEN_INVALID, exception.getStatusCode());
    }

    @Test
    void loginWithGoogle_linksVerifiedSubjectToExistingAccountAndIssuesTokens() {
        var accountId = java.util.UUID.randomUUID();
        Account account = Account.builder()
                .id(accountId)
                .email("customer@example.com")
                .phone("0987654321")
                .passwordHash("local-hash")
                .role(Role.builder().name(AuthServiceImpl.ROLE_CUSTOMER).build())
                .status(AuthServiceImpl.STATUS_ACTIVE)
                .build();
        GoogleIdentity identity = new GoogleIdentity("google-sub-123", "customer@example.com");

        when(googleIdentityVerifier.verify("google-id-token")).thenReturn(identity);
        when(accountRepository.findByGoogleSubject(identity.subject())).thenReturn(java.util.Optional.empty());
        when(accountRepository.findByEmailIgnoreCase(identity.email())).thenReturn(java.util.Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(employeeRepository.findByAccountId(accountId)).thenReturn(java.util.Optional.empty());
        when(customerRepository.findByAccountId(accountId)).thenReturn(java.util.Optional.empty());
        when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(86_400_000L);
        when(tokenProvider.generateAccessToken(accountId, account.getEmail(), AuthServiceImpl.ROLE_CUSTOMER, null))
                .thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(accountId, account.getEmail())).thenReturn("refresh-token");

        var response = authService.loginWithGoogle(
                GoogleLoginRequest.builder().idToken("google-id-token").build()
        );

        assertEquals(identity.subject(), account.getGoogleSubject());
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(accountRepository).save(account);
    }

    @Test
    void loginWithGoogle_usesProviderSubjectAfterAccountWasLinked() {
        var accountId = java.util.UUID.randomUUID();
        Account account = Account.builder()
                .id(accountId)
                .email("customer@example.com")
                .phone("0987654321")
                .passwordHash("local-hash")
                .googleSubject("google-sub-123")
                .role(Role.builder().name(AuthServiceImpl.ROLE_CUSTOMER).build())
                .status(AuthServiceImpl.STATUS_ACTIVE)
                .build();

        when(googleIdentityVerifier.verify("google-id-token"))
                .thenReturn(new GoogleIdentity("google-sub-123", "customer@example.com"));
        when(accountRepository.findByGoogleSubject("google-sub-123")).thenReturn(java.util.Optional.of(account));
        when(employeeRepository.findByAccountId(accountId)).thenReturn(java.util.Optional.empty());
        when(customerRepository.findByAccountId(accountId)).thenReturn(java.util.Optional.empty());
        when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(86_400_000L);
        when(tokenProvider.generateAccessToken(accountId, account.getEmail(), AuthServiceImpl.ROLE_CUSTOMER, null))
                .thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(accountId, account.getEmail())).thenReturn("refresh-token");

        var response = authService.loginWithGoogle(
                GoogleLoginRequest.builder().idToken("google-id-token").build()
        );

        assertEquals("access-token", response.getAccessToken());
        verify(accountRepository, org.mockito.Mockito.never()).findByEmailIgnoreCase("customer@example.com");
    }

    @Test
    void loginWithGoogle_rejectsGoogleIdentityWithoutExistingLocalAccount() {
        GoogleIdentity identity = new GoogleIdentity("google-sub-404", "new@example.com");
        when(googleIdentityVerifier.verify("google-id-token")).thenReturn(identity);
        when(accountRepository.findByGoogleSubject(identity.subject())).thenReturn(java.util.Optional.empty());
        when(accountRepository.findByEmailIgnoreCase(identity.email())).thenReturn(java.util.Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.loginWithGoogle(
                        GoogleLoginRequest.builder().idToken("google-id-token").build()
                )
        );

        assertEquals(StatusCode.GOOGLE_ACCOUNT_NOT_LINKED, exception.getStatusCode());
    }

    @Test
    void loginWithGoogle_rejectsLockedExistingAccount() {
        Account account = Account.builder()
                .id(java.util.UUID.randomUUID())
                .email("locked@example.com")
                .phone("0987654321")
                .passwordHash("local-hash")
                .role(Role.builder().name(AuthServiceImpl.ROLE_CUSTOMER).build())
                .status(AuthServiceImpl.STATUS_LOCKED)
                .build();
        when(googleIdentityVerifier.verify("google-id-token"))
                .thenReturn(new GoogleIdentity("google-sub-locked", account.getEmail()));
        when(accountRepository.findByGoogleSubject("google-sub-locked")).thenReturn(java.util.Optional.empty());
        when(accountRepository.findByEmailIgnoreCase(account.getEmail())).thenReturn(java.util.Optional.of(account));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.loginWithGoogle(
                        GoogleLoginRequest.builder().idToken("google-id-token").build()
                )
        );

        assertEquals(StatusCode.ACCOUNT_LOCKED, exception.getStatusCode());
        verify(accountRepository, org.mockito.Mockito.never()).save(account);
    }
}
