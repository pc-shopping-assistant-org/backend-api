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
    public static final String ACCOUNT_REVOKED_BEFORE_PREFIX = "account:tokens-revoked-before:";

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
                // 1. Check if token is in Redis blacklist. Revocation lookup
                // failures fail closed: a token must not authenticate while
                // the session store is unavailable.
                if (isTokenBlacklisted(jwt)) {
                    log.warn("Attempt to authenticate with blacklisted token");
                    filterChain.doFilter(request, response);
                    return;
                }

                // 2. Decode the principal from JWT claims in memory. The next
                // status check still consults the authoritative account row.
                UserPrincipal userPrincipal = tokenProvider.getUserPrincipalFromToken(jwt);

                // 3. Check the Redis revocation hint and then the database
                // source of truth. The JWT status claim is intentionally not
                // trusted because it is stale after an admin lock/unlock.
                if (userPrincipal.getAccountId() != null && isAccountBlocked(userPrincipal.getAccountId())) {
                    log.warn("Attempt to authenticate with blocked account: {}", userPrincipal.getAccountId());
                    filterChain.doFilter(request, response);
                    return;
                }

                // Logout revokes every token issued before the logout cutoff
                // second for the account. This protects the optional
                // refresh-token path without storing a second long-lived
                // session identifier.
                if (userPrincipal.getAccountId() != null
                        && isAccountTokenRevoked(userPrincipal.getAccountId(), jwt)) {
                    log.warn("Attempt to authenticate with a token issued before account logout: {}",
                            userPrincipal.getAccountId());
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
            log.error("Redis unavailable during token blacklist check. Failing closed: {}", ex.getMessage());
            return true;
        }
    }

    private boolean isAccountBlocked(UUID accountId) {
        boolean cacheBlocked = false;
        try {
            Boolean isBlocked = redisTemplate.hasKey(BLOCKED_ACCOUNT_PREFIX + accountId);
            cacheBlocked = Boolean.TRUE.equals(isBlocked);
        } catch (Exception ex) {
            log.warn("Redis unavailable during account block check; querying DB for account [{}]: {}", accountId, ex.getMessage());
        }
        if (cacheBlocked) {
            return true;
        }

        try {
            return accountRepository.findStatusById(accountId)
                    .map(status -> !"ACTIVE".equalsIgnoreCase(status))
                    .orElse(true);
        } catch (Exception dbEx) {
            // Do not authenticate when the authoritative status cannot be
            // checked; a transient DB failure must not turn a locked account
            // into an active one.
            log.error("DB account status check failed for account [{}]", accountId, dbEx);
            return true;
        }
    }

    private boolean isAccountTokenRevoked(UUID accountId, String token) {
        String key = ACCOUNT_REVOKED_BEFORE_PREFIX + accountId;
        try {
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                return false;
            }

            String revokedBeforeValue = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(revokedBeforeValue)) {
                log.error("Account revocation marker is empty for account [{}]", accountId);
                return true;
            }

            long revokedBefore = Long.parseLong(revokedBeforeValue);
            // JWT NumericDate values have second precision. Keep the cutoff
            // strict so a fresh login in the same second as logout survives.
            long issuedAtSecond = tokenProvider.getIssuedAtTimeMsFromToken(token) / 1000L;
            return issuedAtSecond < revokedBefore;
        } catch (Exception ex) {
            log.error("Redis unavailable during account token revocation check. Failing closed: {}",
                    ex.getMessage());
            return true;
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
