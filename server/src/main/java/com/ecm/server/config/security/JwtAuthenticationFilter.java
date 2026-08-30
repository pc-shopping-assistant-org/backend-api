package com.ecm.server.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String BLACKLIST_TOKEN_PREFIX = "token:blacklist:";
    public static final String BLOCKED_ACCOUNT_PREFIX = "account:blocked:";

    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final com.ecm.server.repository.AccountRepository accountRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // 1. Check if token is in Redis blacklist (with fail-open resilience)
                if (isTokenBlacklisted(jwt)) {
                    log.warn("Attempt to authenticate with blacklisted token");
                    filterChain.doFilter(request, response);
                    return;
                }

                // 2. Decode UserPrincipal directly from JWT claims in memory (0 DB queries)
                UserPrincipal userPrincipal = tokenProvider.getUserPrincipalFromToken(jwt);

                // 3. Check if account is in Redis blocked list (with fail-open resilience)
                if (userPrincipal.getAccountId() != null && isAccountBlocked(userPrincipal.getAccountId())) {
                    log.warn("Attempt to authenticate with blocked account: {}", userPrincipal.getAccountId());
                    filterChain.doFilter(request, response);
                    return;
                }

                // 4. Set in-memory authentication in SecurityContext
                if (userPrincipal.isEnabled() && userPrincipal.isAccountNonLocked()) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userPrincipal,
                            null,
                            userPrincipal.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTokenBlacklisted(String token) {
        try {
            Boolean isBlacklisted = redisTemplate.hasKey(BLACKLIST_TOKEN_PREFIX + token);
            return Boolean.TRUE.equals(isBlacklisted);
        } catch (Exception ex) {
            log.warn("Redis unavailable during token blacklist check. Failing open: {}", ex.getMessage());
            return false;
        }
    }

    private boolean isAccountBlocked(UUID accountId) {
        try {
            Boolean isBlocked = redisTemplate.hasKey(BLOCKED_ACCOUNT_PREFIX + accountId);
            return Boolean.TRUE.equals(isBlocked);
        } catch (Exception ex) {
            log.warn("Redis unavailable during account block check. Fallback querying DB PostgreSQL for account [{}]: {}", accountId, ex.getMessage());
            try {
                return accountRepository.findById(accountId)
                        .map(account -> !"ACTIVE".equalsIgnoreCase(account.getStatus()))
                        .orElse(true);
            } catch (Exception dbEx) {
                log.error("DB fallback check failed for account [{}]: {}", accountId, dbEx.getMessage());
                return false;
            }
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
