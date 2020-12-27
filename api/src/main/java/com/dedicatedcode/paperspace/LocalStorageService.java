package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.Document;
import com.dedicatedcode.paperspace.model.OCRState;
import com.dedicatedcode.paperspace.model.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {
    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);
    private final File storageLocation;
    private final File documentsLocation;
    private final File tasksLocation;

    public LocalStorageService(@Value("${storage.folder.binaries}") File localStoragePath,
                               @Value("${storage.folder.documents.upload}") File documentsLocation,
                               @Value("${storage.folder.tasks.upload}") File tasksLocation) {
        this.storageLocation = localStoragePath;
        this.documentsLocation = documentsLocation;
        this.tasksLocation = tasksLocation;
        createFolderIfNecessary(storageLocation);
        createFolderIfNecessary(documentsLocation);
        createFolderIfNecessary(tasksLocation);

    }

    private void createFolderIfNecessary(File path) {
        if (path.isDirectory()) {
            log.debug("storage folder [{}] exists", path);
        } else {
            try {
                Files.createDirectories(path.toPath());
            } catch (IOException e) {
                log.error("Unable to create storage folder [{}]", path);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Binary store(UploadType type, InputStream inputStream, String originalFileName, String mimeType) {
        File storageFolder;
        switch (type) {
            case DOCUMENT:
                storageFolder = this.documentsLocation;
                break;
            case TASK:
                storageFolder = this.tasksLocation;
                break;
            case IMAGE:
                storageFolder = this.storageLocation;
                break;
            default:
                throw new RuntimeException("Unhandled upload type [" + type + "]");
        }
        String finalName = determineStorageFileName(storageFolder, originalFileName, 1);
        File outputFile = new File(storageFolder, finalName);
        try {
            FileCopyUtils.copy(inputStream, new FileOutputStream(outputFile));
            String md5;
            try (InputStream is = new FileInputStream(outputFile)) {
                md5 = DigestUtils.md5DigestAsHex(is);
            } catch (IOException e) {
                log.error("could not calculate md5 sum of [{}]", outputFile);
                throw new StorageException(e);
            }
            log.info("stored binary under [{}] with original file name [{}]", outputFile, originalFileName);
            return new Binary(UUID.randomUUID(), LocalDateTime.now(), outputFile.getAbsolutePath(), md5, mimeType,
                    Files.size(outputFile.toPath()), type == UploadType.IMAGE ? OCRState.UNNECESSARY : OCRState.OPEN);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Binary store(UploadType type, File file, String originalFileName, String mimeType) {
        try {
            return store(type, new FileInputStream(file), originalFileName, mimeType);
        } catch (FileNotFoundException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Binary store(UploadType type, MultipartFile multipartFile, String originalFileName, String mimeType) {
        try {
            return store(type, multipartFile.getInputStream(), originalFileName, mimeType);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    private String determineStorageFileName(File folder, String originalFileName, int iteration) {
        File targetFile = new File(folder, originalFileName);
        if (!targetFile.exists()) {
            return targetFile.getName();
        } else {
            String newFileName;
            int extensionsStart = originalFileName.lastIndexOf('.');
            if (extensionsStart != -1) {
                newFileName = originalFileName.substring(0, extensionsStart) + "_" + iteration + originalFileName.substring(extensionsStart);
            } else {
                newFileName = originalFileName + "_" + iteration;
            }
            targetFile = new File(folder, newFileName);
            if (!targetFile.exists()) {
                return targetFile.getName();
            } else {
                return determineStorageFileName(folder, originalFileName, iteration + 1);
            }
        }
    }

    @Override
    public void delete(Document document) {
        Set<String> filesToDelete = new HashSet<>();
        filesToDelete.add(document.getFile().getStorageLocation());
        document.getPages().stream().map(Page::getPreview).forEach(binary -> {
            filesToDelete.add(binary.getStorageLocation());
            File previewFile = new File(binary.getStorageLocation());
            String startFileName = previewFile.getName().substring(0, previewFile.getName().lastIndexOf('.'));
            try {
                Files.walk(storageLocation.toPath())
                        .filter(path -> path.toFile().getName().startsWith(startFileName))
                        .forEach(path -> filesToDelete.add(path.toAbsolutePath().toString()));
            } catch (IOException e) {
                log.error("Could not traverse binary location to find files for deletion", e);
            }
        });
        filesToDelete.stream().map(File::new).forEach(file -> {
            if(file.delete()) {
                log.debug("deleted [{}]", file);
            } else {
                log.debug("deletion of [{}] returned false, will schedule deletionOnExit", file);
                file.deleteOnExit();
            }
        } );
    }

    @Override
    public void delete(String path) {
        boolean delete = new File(path).delete();
        if (!delete) {
            new File(path).deleteOnExit();
        }
    }
}
