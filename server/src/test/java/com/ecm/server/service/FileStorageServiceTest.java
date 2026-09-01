package com.ecm.server.service;

import com.ecm.server.config.FileStorageProperties;
import com.ecm.server.dto.response.FileResponse;
import com.ecm.server.model.File;
import com.ecm.server.repository.FileRepository;
import com.ecm.server.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private FileRepository fileRepository;

    @TempDir
    private Path uploadRoot;

    private FileStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setRoot(uploadRoot);
        properties.setPublicBaseUrl("http://localhost:8080/api/v1/files");
        properties.setMaxSizeBytes(1024);
        lenient().when(fileRepository.save(any(File.class))).thenAnswer(invocation -> {
            File file = invocation.getArgument(0);
            if (file.getId() == null) file.setId(UUID.randomUUID());
            if (file.getCreatedAt() == null) file.setCreatedAt(Instant.now());
            return file;
        });
        service = new FileStorageServiceImpl(fileRepository, properties);
    }

    @Test
    void uploadImageStoresMetadataAndReturnsPublicUrl() throws Exception {
        MockMultipartFile upload = new MockMultipartFile(
                "file", "hero.png", "image/png", new byte[]{1, 2, 3}
        );

        FileResponse response = service.uploadImage(upload);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getOriginalName()).isEqualTo("hero.png");
        assertThat(response.getMimeType()).isEqualTo("image/png");
        assertThat(response.getPublicUrl()).startsWith("http://localhost:8080/api/v1/files/")
                .endsWith("/content");
        assertThat(Files.list(uploadRoot)).hasSize(1);
    }

    @Test
    void uploadImageRejectsUnsupportedMimeType() {
        MockMultipartFile upload = new MockMultipartFile(
                "file", "payload.svg", "image/svg+xml", new byte[]{1}
        );

        assertThatThrownBy(() -> service.uploadImage(upload))
                .hasMessageContaining("Only JPEG, PNG, WEBP or GIF");
    }

    @Test
    void uploadImageRejectsFilesAboveConfiguredLimit() {
        MockMultipartFile upload = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", new byte[1025]
        );

        assertThatThrownBy(() -> service.uploadImage(upload))
                .hasMessageContaining("maximum upload size");
    }
}
