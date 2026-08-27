package com.ecm.server.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secretKey = "dGhpc19pc19hX3Zlcnlfc2VjdXJlX2tleV9mb3Jfand0X3NpZ25pbmdfYXV0aGVudGljYXRpb25fMjAyNg==";
    private long accessTokenExpirationMs = 86400000L; // 24 hours
    private long refreshTokenExpirationMs = 604800000L; // 7 days
}
