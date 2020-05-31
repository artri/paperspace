package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

public interface StorageService {
    Binary store(MultipartFile multipartFile, String originalFileName, String mimeType);

    File load(UUID id);
}
