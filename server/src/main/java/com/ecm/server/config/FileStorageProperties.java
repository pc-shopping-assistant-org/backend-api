package com.ecm.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Local media storage configuration used by the development/admin upload
 * flow. The database remains the source of metadata while the file content is
 * kept outside the repository and can be replaced by an object-storage
 * adapter later.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file-storage")
public class FileStorageProperties {

    private Path root = Path.of("./var/uploads");
    private String publicBaseUrl = "http://localhost:8080/api/v1/files";
    private long maxSizeBytes = 10 * 1024 * 1024;
}
