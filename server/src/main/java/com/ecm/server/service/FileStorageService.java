package com.ecm.server.service;

import com.ecm.server.dto.response.FileResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface FileStorageService {

    FileResponse uploadImage(MultipartFile file);

    StoredFile open(UUID fileId);

    record StoredFile(Resource resource, String mimeType, String originalName) {
    }
}
