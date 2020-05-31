package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {
    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);
    private final File storageLocation;

    public LocalStorageService(@Value("${storage.local.binary}") String localStoragePath) {
        storageLocation = new File(localStoragePath);
        if (storageLocation.isDirectory()) {
            log.debug("storage folder [{}] exists", storageLocation);
        } else {
            try {
                Files.createDirectories(storageLocation.toPath());
            } catch (IOException e) {
                log.error("Unable to create storage folder [{}]", storageLocation);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Binary store(MultipartFile multipartFile, String originalFileName, String mimeType) {
        UUID finalName = UUID.randomUUID();
        File outputFile = new File(storageLocation, finalName.toString());
        try {
            FileCopyUtils.copy(multipartFile.getBytes(), outputFile);
            return new Binary(finalName, LocalDateTime.now(), originalFileName, mimeType,
                    Files.size(outputFile.toPath()));
        } catch (IOException e) {
            throw new StorageException(e);
        }

    }

    @Override
    public File load(UUID id) {
        return new File(storageLocation, id.toString());
    }
}
