package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class LocalStorageServiceTest {
    @Autowired
    private LocalStorageService service;

    @Value("${storage.folder.documents.upload}")
    private File documentUploadPath;

    @Test
    void shouldCreateNumberedVersions() throws IOException {
        UUID name = UUID.randomUUID();
        String originalFilename = name + ".pdf";
        String mimeType = "application/pdf";
        MockMultipartFile file = new MockMultipartFile("data", originalFilename, mimeType, getClass().getResourceAsStream("/test_files/A Sample PDF.pdf"));
        Binary stored = service.store(UploadType.DOCUMENT, file, originalFilename, mimeType);
        assertEquals(documentUploadPath.getAbsolutePath() + File.separatorChar + originalFilename, stored.getStorageLocation());
        stored = service.store(UploadType.DOCUMENT, file, originalFilename, mimeType);
        assertEquals(documentUploadPath.getAbsolutePath() + File.separatorChar + name + "_1.pdf", stored.getStorageLocation());
        stored = service.store(UploadType.DOCUMENT, file, originalFilename, mimeType);
        assertEquals(documentUploadPath.getAbsolutePath() + File.separatorChar + name + "_2.pdf", stored.getStorageLocation());
    }
}