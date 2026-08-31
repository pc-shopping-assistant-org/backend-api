package com.ecm.server.config.security;

import com.ecm.server.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void lockedAccountIsRejectedEvenWhenJwtStatusAndRedisHintAreStale() throws Exception {
        UUID accountId = UUID.randomUUID();
        String token = "valid-token";
        UserPrincipal principal = UserPrincipal.builder()
                .accountId(accountId)
                .username("user@example.com")
                .status("ACTIVE")
                .authorities(java.util.List.of())
                .build();
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserPrincipalFromToken(token)).thenReturn(principal);
        when(redisTemplate.hasKey(JwtAuthenticationFilter.BLACKLIST_TOKEN_PREFIX + token)).thenReturn(false);
        when(redisTemplate.hasKey(JwtAuthenticationFilter.BLOCKED_ACCOUNT_PREFIX + accountId)).thenReturn(false);
        when(accountRepository.findStatusById(accountId)).thenReturn(Optional.of("LOCKED"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + token);

        new JwtAuthenticationFilter(tokenProvider, redisTemplate, accountRepository)
                .doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(eq(request), any());
    }

    @Test
    void activeAccountIsAuthenticatedAfterAuthoritativeStatusCheck() throws Exception {
        UUID accountId = UUID.randomUUID();
        String token = "valid-token";
        UserPrincipal principal = UserPrincipal.builder()
                .accountId(accountId)
                .username("user@example.com")
                .status("ACTIVE")
                .authorities(java.util.List.of())
                .build();
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserPrincipalFromToken(token)).thenReturn(principal);
        when(redisTemplate.hasKey(JwtAuthenticationFilter.BLACKLIST_TOKEN_PREFIX + token)).thenReturn(false);
        when(redisTemplate.hasKey(JwtAuthenticationFilter.BLOCKED_ACCOUNT_PREFIX + accountId)).thenReturn(false);
        when(accountRepository.findStatusById(accountId)).thenReturn(Optional.of("ACTIVE"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + token);

        new JwtAuthenticationFilter(tokenProvider, redisTemplate, accountRepository)
                .doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(eq(request), any());
    }

    @Test
    void redisBlacklistOutageDoesNotAuthenticateAccessToken() throws Exception {
        String token = "valid-token";
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(redisTemplate.hasKey(JwtAuthenticationFilter.BLACKLIST_TOKEN_PREFIX + token))
                .thenThrow(new IllegalStateException("redis down"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + token);

        new JwtAuthenticationFilter(tokenProvider, redisTemplate, accountRepository)
                .doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(eq(request), any());
    }

    @Test
    void tokenIssuedBeforeAccountLogoutIsRejected() throws Exception {
        UUID accountId = UUID.randomUUID();
        String token = "valid-token";
        UserPrincipal principal = UserPrincipal.builder()
                .accountId(accountId)
                .username("user@example.com")
                .status("ACTIVE")
                .authorities(java.util.List.of())
                .build();
        String revocationKey = JwtAuthenticationFilter.ACCOUNT_REVOKED_BEFORE_PREFIX + accountId;
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserPrincipalFromToken(token)).thenReturn(principal);
        when(redisTemplate.hasKey(JwtAuthenticationFilter.BLACKLIST_TOKEN_PREFIX + token)).thenReturn(false);
        when(redisTemplate.hasKey(JwtAuthenticationFilter.BLOCKED_ACCOUNT_PREFIX + accountId)).thenReturn(false);
        when(accountRepository.findStatusById(accountId)).thenReturn(Optional.of("ACTIVE"));
        when(redisTemplate.hasKey(revocationKey)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(revocationKey)).thenReturn("2000");
        when(tokenProvider.getIssuedAtTimeMsFromToken(token)).thenReturn(1000L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + token);

        new JwtAuthenticationFilter(tokenProvider, redisTemplate, accountRepository)
                .doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(eq(request), any());
    }

    @Test
    void tokenIssuedInLogoutCutoffSecondIsNotRejected() throws Exception {
        UUID accountId = UUID.randomUUID();
        String token = "valid-token";
        UserPrincipal principal = UserPrincipal.builder()
                .accountId(accountId)
                .username("user@example.com")
                .status("ACTIVE")
                .authorities(java.util.List.of())
                .build();
        String revocationKey = JwtAuthenticationFilter.ACCOUNT_REVOKED_BEFORE_PREFIX + accountId;
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserPrincipalFromToken(token)).thenReturn(principal);
        when(redisTemplate.hasKey(JwtAuthenticationFilter.BLACKLIST_TOKEN_PREFIX + token)).thenReturn(false);
        when(redisTemplate.hasKey(JwtAuthenticationFilter.BLOCKED_ACCOUNT_PREFIX + accountId)).thenReturn(false);
        when(accountRepository.findStatusById(accountId)).thenReturn(Optional.of("ACTIVE"));
        when(redisTemplate.hasKey(revocationKey)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(revocationKey)).thenReturn("2");
        when(tokenProvider.getIssuedAtTimeMsFromToken(token)).thenReturn(2_000L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + token);

        new JwtAuthenticationFilter(tokenProvider, redisTemplate, accountRepository)
                .doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(eq(request), any());
    }
}
