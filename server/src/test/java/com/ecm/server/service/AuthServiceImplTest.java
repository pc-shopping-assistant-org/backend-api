package com.ecm.server.service;

import com.ecm.server.config.security.JwtAuthenticationFilter;
import com.ecm.server.config.security.JwtProperties;
import com.ecm.server.config.security.JwtTokenProvider;
import com.ecm.server.mapper.UserMapper;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.CustomerAddressRepository;
import com.ecm.server.repository.CustomerRepository;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.RoleRepository;
import com.ecm.server.service.impl.AuthServiceImpl;
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
}
