package com.ecm.server.repository;

import com.ecm.server.model.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID> {
    Optional<File> findByStorageProviderAndStorageKey(String storageProvider, String storageKey);
}
