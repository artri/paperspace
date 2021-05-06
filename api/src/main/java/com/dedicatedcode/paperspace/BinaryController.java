package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.OCRState;
import com.dedicatedcode.paperspace.web.BinaryResponse;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.UUID;

@RestController
public class BinaryController {
    private static final Logger log = LoggerFactory.getLogger(BinaryController.class);
    private final StorageService storageService;
    private final BinaryService binaryService;

    @Autowired
    public BinaryController(StorageService storageService, BinaryService binaryService) {
        this.storageService = storageService;
        this.binaryService = binaryService;
    }

    @PostMapping(value = "/api/binary", produces = "application/json")
    public ResponseEntity<BinaryResponse> upload(
            @RequestParam(value = "mimeType") String mimeType,
            @RequestParam(value = "file") MultipartFile file,
            @RequestParam(value = "type") UploadType type) {
        log.info("processing new binary upload [{}] with type [{}]", file.getOriginalFilename(), mimeType);
        try {
            File tmpFile = File.createTempFile("upload", "tmp");
            FileCopyUtils.copy(file.getInputStream(), new FileOutputStream(tmpFile));
            String md5;
            try (InputStream is = new FileInputStream(tmpFile)) {
                md5 = DigestUtils.md5DigestAsHex(is);
            } catch (IOException e) {
                log.error("could not calculate md5 sum of [{}]", tmpFile);
                throw new StorageException(e);
            }
            Binary byHash = this.binaryService.getByHash(md5);
            if (byHash != null) {
                log.info("Binary already uploaded");
                return ResponseEntity.status(HttpStatus.SEE_OTHER).body(new BinaryResponse(byHash));
            } else {
                Binary binary = this.storageService.store(type, tmpFile, file.getOriginalFilename(), mimeType);
                return ResponseEntity.status(HttpStatus.CREATED).body(new BinaryResponse(binary));
            }
        } catch (IOException e) {
            log.error("Could not write tmp file");
            throw new StorageException(e);
        }
    }

    @RequestMapping(value = "/api/download/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        Binary binary = this.binaryService.get(id);
        if (binary == null) {
            throw new UnknownBinaryException("could not find binary with id [" + id + "]");
        }

        FileSystemResource resource = new FileSystemResource(binary.getStorageLocation());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", new File(binary.getStorageLocation()).getName());
        headers.setContentType(MediaType.parseMediaType(binary.getMimeType()));
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @RequestMapping(value = "/api/view/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Resource> view(@PathVariable UUID id) {
        Binary binary = this.binaryService.get(id);
        if (binary == null) {
            throw new UnknownBinaryException("could not find binary with id [" + id + "]");
        }

        FileSystemResource resource = new FileSystemResource(binary.getStorageLocation());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(binary.getMimeType()));
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @RequestMapping(value = "/api/image/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Resource> image(@PathVariable UUID id, @RequestParam(required = false) Integer width) {
        Binary binary = this.binaryService.get(id);
        if (binary == null) {
            throw new UnknownBinaryException("could not find binary with id [" + id + "]");
        }

        File loadedFile;
        ;

        if (width != null) {
            File originalFile = new File(binary.getStorageLocation());
            File scaledFile = new File(originalFile.getParentFile(), getScaledFileName(originalFile, width));
            if (scaledFile.exists()) {
                log.debug("Found existing scaled file for [{}] with width [{}]", originalFile, width);
                loadedFile = scaledFile;
            } else {
                try {
                    log.debug("Scaled file for [{}] with width [{}] will be created", originalFile, width);
                    Thumbnails.of(originalFile).width(width).toFile(scaledFile);
                    loadedFile = scaledFile;
                } catch (IOException e) {
                    log.error("Could not scale image from file [{}] to width [{}]", originalFile, width, e);
                    loadedFile = originalFile;
                }
            }
        } else {
            loadedFile = new File(binary.getStorageLocation());
        }

        FileSystemResource resource = new FileSystemResource(loadedFile);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(binary.getMimeType()));
        return ResponseEntity.ok()
                .lastModified(loadedFile.lastModified())
                .headers(headers)
                .body(resource);
    }

    @PostMapping("/api/binary/{id}/delete")
    @ResponseBody
    public ResponseEntity<BinaryResponse> delete(@PathVariable UUID id) {
        Binary binary = this.binaryService.get(id);
        if (binary == null) {
            throw new UnknownBinaryException("could not find binary with id [" + id + "]");
        }
        this.binaryService.delete(binary);
        this.storageService.delete(binary.getStorageLocation());
        return ResponseEntity.ok(new BinaryResponse(binary));
    }

    @PostMapping("/api/binary/{id}/ignore")
    @ResponseBody
    public ResponseEntity<BinaryResponse> ignore(@PathVariable UUID id) {
        Binary binary = this.binaryService.get(id);
        if (binary == null) {
            throw new UnknownBinaryException("could not find binary with id [" + id + "]");
        }
        Binary newBinary = binary.withState(OCRState.IGNORED);
        this.binaryService.update(newBinary);
        return ResponseEntity.ok(new BinaryResponse(newBinary));
    }

    private String getScaledFileName(File originalFile, Integer width) {
        String originalName = originalFile.getName();
        if (originalName.contains(".")) {
            return originalName.substring(0, originalName.lastIndexOf(".")) + "_w_" + width + originalName.substring(originalName.lastIndexOf("."));
        } else {
            return originalName + "_w_" + width;
        }
    }

}
