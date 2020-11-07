package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.Document;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.UUID;

public interface StorageService {
    Binary store(UploadType type, InputStream is, String originalFileName, String mimeType);

    Binary store(UploadType type, File file, String originalFileName, String mimeType);

    Binary store(UploadType type, MultipartFile multipartFile, String originalFileName, String mimeType);

    void delete(String path);

    void delete(Document document);
}
