package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/binary", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public Binary upload(
            @RequestParam(value = "mimeType") String mimeType,
            @RequestParam(value = "data") MultipartFile file) {
        log.info("processing new binary upload [{}] with type [{}]", file.getOriginalFilename(), mimeType);
        Binary binary = this.storageService.store(file, file.getOriginalFilename(), mimeType);
        this.binaryService.store(binary);
        return binary;
    }

    @RequestMapping(value = "/download/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        Binary binary = this.binaryService.get(id);
        if (binary==null) {
            throw new UnknownBinaryException("could not find binary with id ["+id+"]");
        }

        FileSystemResource resource = new FileSystemResource(this.storageService.load(id));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", binary.getOriginalFileName());
        headers.setContentType(MediaType.parseMediaType(binary.getMimeType()));
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @RequestMapping(value = "/view/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Resource> view(@PathVariable UUID id) {
        Binary binary = this.binaryService.get(id);
        if (binary==null) {
            throw new UnknownBinaryException("could not find binary with id ["+id+"]");
        }

        FileSystemResource resource = new FileSystemResource(this.storageService.load(id));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(binary.getMimeType()));
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @ExceptionHandler(UnknownBinaryException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(UnknownBinaryException ex) {
        return new ErrorResponse(ex.getMessage(), 404);
    }
}
