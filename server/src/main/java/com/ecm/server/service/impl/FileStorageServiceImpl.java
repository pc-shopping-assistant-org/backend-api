package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.config.FileStorageProperties;
import com.ecm.server.dto.response.FileResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.model.File;
import com.ecm.server.repository.FileRepository;
import com.ecm.server.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final String LOCAL_PROVIDER = "LOCAL";
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final FileRepository fileRepository;
    private final FileStorageProperties properties;

    @Override
    @Transactional
    public FileResponse uploadImage(MultipartFile multipartFile) {
        validate(multipartFile);

        Path root = properties.getRoot().toAbsolutePath().normalize();
        String originalName = safeOriginalName(multipartFile.getOriginalFilename());
        String extension = extensionFor(multipartFile.getContentType(), originalName);
        String storageKey = UUID.randomUUID() + extension;
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(StatusCode.VALIDATION_ERROR, "Invalid file path");
        }

        try {
            Files.createDirectories(root);
            try (InputStream input = multipartFile.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }

            File saved = fileRepository.save(File.builder()
                    .storageProvider(LOCAL_PROVIDER)
                    .storageKey(storageKey)
                    .originalName(originalName)
                    .mimeType(multipartFile.getContentType().toLowerCase(Locale.ROOT))
                    .sizeBytes(multipartFile.getSize())
                    .status("ACTIVE")
                    .build());

            saved.setPublicUrl(publicUrl(saved.getId()));
            saved = fileRepository.save(saved);
            return toResponse(saved);
        } catch (IOException | RuntimeException ex) {
            deleteQuietly(target);
            if (ex instanceof BusinessException businessException) {
                throw businessException;
            }
            log.error("Failed to store uploaded file {}", originalName, ex);
            throw new BusinessException(StatusCode.SERVICE_UNAVAILABLE,
                    "File storage is temporarily unavailable");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile open(UUID fileId) {
        File file = fileRepository.findById(fileId)
                .filter(candidate -> LOCAL_PROVIDER.equalsIgnoreCase(candidate.getStorageProvider()))
                .filter(candidate -> "ACTIVE".equalsIgnoreCase(candidate.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.IMAGE_NOT_FOUND));

        Path root = properties.getRoot().toAbsolutePath().normalize();
        Path path = root.resolve(file.getStorageKey()).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new BusinessException(StatusCode.IMAGE_NOT_FOUND);
        }
        Resource resource = new FileSystemResource(path);
        return new StoredFile(resource, file.getMimeType(), file.getOriginalName());
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(StatusCode.VALIDATION_ERROR,
                    "An image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(StatusCode.VALIDATION_ERROR,
                    "Only JPEG, PNG, WEBP or GIF images are supported");
        }
        if (file.getSize() > properties.getMaxSizeBytes()) {
            throw new BusinessException(StatusCode.VALIDATION_ERROR,
                    "Image exceeds the maximum upload size of "
                            + properties.getMaxSizeBytes() / (1024 * 1024) + " MB");
        }
    }

    private String safeOriginalName(String name) {
        String cleaned = StringUtils.cleanPath(name == null ? "upload" : name);
        String basename = Path.of(cleaned).getFileName().toString();
        if (basename.isBlank() || basename.contains("..")) {
            return "upload";
        }
        return basename.length() > 255 ? basename.substring(0, 255) : basename;
    }

    private String extensionFor(String contentType, String originalName) {
        String extension = StringUtils.getFilenameExtension(originalName);
        if (extension != null && extension.matches("[A-Za-z0-9]{1,8}")) {
            return "." + extension.toLowerCase(Locale.ROOT);
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
    }

    private String publicUrl(UUID id) {
        String base = properties.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            base = "/api/v1/files";
        }
        return base.replaceAll("/+$", "") + "/" + id + "/content";
    }

    private FileResponse toResponse(File file) {
        return FileResponse.builder()
                .id(file.getId())
                .storageProvider(file.getStorageProvider())
                .originalName(file.getOriginalName())
                .mimeType(file.getMimeType())
                .sizeBytes(file.getSizeBytes())
                .publicUrl(file.getPublicUrl())
                .status(file.getStatus())
                .createdAt(file.getCreatedAt())
                .build();
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException cleanupException) {
            log.warn("Could not clean up failed upload {}", path, cleanupException);
        }
    }
}
