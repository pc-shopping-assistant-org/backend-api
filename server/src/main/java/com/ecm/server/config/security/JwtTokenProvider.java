package com.ecm.server.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UUID accountId, String identifier, String role, UUID employeeId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpirationMs());

        var builder = Jwts.builder()
                .subject(identifier)
                .claim("accountId", accountId != null ? accountId.toString() : null)
                .claim("role", role)
                .claim("status", "ACTIVE")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key);

        if (employeeId != null) {
            builder.claim("employeeId", employeeId.toString());
        }

        return builder.compact();
    }

    public String generateAccessToken(UUID accountId, String identifier, String role) {
        return generateAccessToken(accountId, identifier, role, null);
    }

    public String generateRefreshToken(UUID accountId, String identifier) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpirationMs());

        return Jwts.builder()
                .subject(identifier)
                .claim("accountId", accountId != null ? accountId.toString() : null)
                .claim("type", "REFRESH")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public UserPrincipal getUserPrincipalFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String identifier = claims.getSubject();
        String accountIdStr = claims.get("accountId", String.class);
        String employeeIdStr = claims.get("employeeId", String.class);
        String role = claims.get("role", String.class);
        String status = claims.get("status", String.class);
        if (status == null) {
            status = "ACTIVE";
        }

        UUID accountId = accountIdStr != null ? UUID.fromString(accountIdStr) : null;
        UUID employeeId = employeeIdStr != null ? UUID.fromString(employeeIdStr) : null;

        List<GrantedAuthority> authorities = role != null
                ? Collections.singletonList(new SimpleGrantedAuthority(role))
                : Collections.emptyList();

        return UserPrincipal.builder()
                .accountId(accountId)
                .employeeId(employeeId)
                .username(identifier)
                .role(role)
                .status(status)
                .authorities(authorities)
                .build();
    }

    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public String getIdentifierFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(getClaimsFromToken(token).get("type", String.class));
    }

    public UUID getAccountIdFromToken(String token) {
        String accountIdStr = getClaimsFromToken(token).get("accountId", String.class);
        return accountIdStr != null ? UUID.fromString(accountIdStr) : null;
    }

    public String getRoleFromToken(String token) {
        return getClaimsFromToken(token).get("role", String.class);
    }

    public long getExpirationTimeMsFromToken(String token) {
        Date expiration = getClaimsFromToken(token).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token is expired: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
        }
        return false;
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
