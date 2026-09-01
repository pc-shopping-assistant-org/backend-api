package com.ecm.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Runtime configuration for Google Identity Services audience validation. */
@Data
@Configuration
@ConfigurationProperties(prefix = "google")
public class GoogleProperties {

    /** OAuth web-client ID accepted as the ID-token audience. */
    private String clientId = "";
}
