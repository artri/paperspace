package com.dedicatedcode.paperspace.feeder;

import com.dedicatedcode.paperspace.BinaryService;
import com.dedicatedcode.paperspace.DocumentService;
import com.dedicatedcode.paperspace.StorageService;
import com.dedicatedcode.paperspace.model.*;
import com.dedicatedcode.paperspace.search.SolrService;
import net.sf.jmimemagic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class DelegatingFileEventListener implements FileEventListener {
    private static final Logger log = LoggerFactory.getLogger(DelegatingFileEventListener.class);
    private final BinaryService binaryService;
    private final DocumentService documentService;
    private final StorageService storageService;
    private final SolrService solrService;
    private final OcrService ocrService;
    private final int defaultDuePeriod;
    private final List<DocumentListener> documentListeners;

    public DelegatingFileEventListener(BinaryService binaryService,
                                       DocumentService documentService,
                                       StorageService storageService,
                                       SolrService solrService,
                                       PdfOcrService ocrService,
                                       @Value("${task.defaultDuePeriod}") int defaultDuePeriod, List<DocumentListener> documentListeners) {
        this.binaryService = binaryService;
        this.documentService = documentService;
        this.storageService = storageService;
        this.solrService = solrService;
        this.ocrService = ocrService;
        this.defaultDuePeriod = defaultDuePeriod;
        this.documentListeners = documentListeners;
    }

    @Override
    public void handle(EventType eventType, File file, InputType inputType) {
        switch (eventType) {
            case DELETE:
                getBinary(file).ifPresent(this::handleDeletion);
                break;
            case MOVE:
                getBinary(file).ifPresent(binary -> handleMove(file, inputType, binary));
                break;
            case CREATE:
                handleCreation(file, inputType);
                break;
            case EXISTING:
                getBinary(file).ifPresentOrElse(this::handleExisting, () -> {
                    log.info("[EXISTING] event received but we dont know the binary, will handle as new");
                    handleCreation(file, inputType);
                });
                break;
            case CHANGE:
                getBinary(file).ifPresent(binary -> handleChange(binary, file));
                break;
        }
    }

    private void handleExisting(Binary binary) {
        String storedHash = binary.getHash();
        File file = new File(binary.getStorageLocation());
        String fileHash = getHashFromFile(file);

        if (storedHash.equals(fileHash)) {
            log.debug("nothing changed for binary [{}]", binary.getId());
        } else {
            log.debug("binary [{}] changed for existing file [{}]. Stored Hash was [{}] and calculated hash is [{}]", binary.getId(), binary.getStorageLocation(), binary.getHash(), fileHash);
            handleChange(binary, file);
        }
    }

    private void handleCreation(File file, InputType inputType) {
        log.info("new file detected. Will doing ocr, creation of screenshots and storing everything in the database");
        String md5 = getHashFromFile(file);
        try {
            List<Page> pages = ocrService.doOcr(file);
            LocalDateTime now = LocalDateTime.now();
            Binary documentBinary = new Binary(UUID.randomUUID(), now, file.getAbsolutePath(), md5, loadMimeType(file), file.length());
            this.binaryService.store(documentBinary);
            pages.forEach(page -> this.binaryService.store(page.getPreview()));

            Document document;
            switch (inputType) {
                case DOCUMENT:
                    document = this.documentService.store(new Document(UUID.randomUUID(), now, file.getName(), null, documentBinary, pages, Collections.emptyList()));
                    break;
                case TASK:
                    document = this.documentService.store(new TaskDocument(UUID.randomUUID(), now, file.getName(), null, documentBinary, State.OPEN, pages, now.plusDays(defaultDuePeriod), null, Collections.emptyList()));
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled input type [" + inputType + "]");
            }
            this.documentListeners.forEach(documentListener -> documentListener.created(document));
            this.solrService.index(document);
        } catch (OcrException e) {
            log.error("processing of [{}] failed with:", file, e);
        }
    }

    private String getHashFromFile(File file) {
        String md5 = null;
        try (InputStream is = new FileInputStream(file)) {
            md5 = DigestUtils.md5DigestAsHex(is);
        } catch (IOException e) {
            log.error("could not calculate md5 sum of [{}]", file);
        }
        return md5;
    }

    private Optional<Binary> getBinary(File inputFile) {
        Binary byPath = this.binaryService.getByPath(inputFile.getAbsolutePath());
        if (byPath == null && inputFile.exists()) {
            try (InputStream is = new FileInputStream(inputFile)) {
                return Optional.ofNullable(this.binaryService.getByHash(DigestUtils.md5DigestAsHex(is)));
            } catch (IOException e) {
                log.error("could not calculate md5 sum of [{}]", inputFile);
                return Optional.empty();
            }
        } else {
            return Optional.ofNullable(byPath);
        }
    }

    private void handleChange(Binary binary, File file) {
        Document associatedDocument = documentService.getByBinary(binary.getId());
        try (InputStream is = new FileInputStream(file)) {
            log.debug("[CHANGE] calculate new hash for binary [{}]", binary.getId());
            this.binaryService.update(binary.withHash(DigestUtils.md5DigestAsHex(is)));
        } catch (IOException e) {
            log.error("could not calculate md5 sum of [{}]", file);
        }
        try {
            List<Page> pages = ocrService.doOcr(file);
            Document updatedDocument = associatedDocument.withPages(pages);
            this.documentService.update(updatedDocument);
            this.solrService.index(updatedDocument);
        } catch (OcrException e) {
            log.error("processing of [{}] failed with:", file, e);
        }
    }

    private void handleDeletion(Binary storedBinary) {
        Optional.ofNullable(documentService.getByBinary(storedBinary.getId())).ifPresent(document -> {
            this.documentService.delete(document);
            log.info("File deletion detected. Will delete everything for document [{}]", document.getId());
            document.getPages().forEach(page -> {
                log.debug("deleting page binary [{}] from database and local storage", page.getPreview().getId());
                this.binaryService.delete(page.getPreview());
                this.storageService.delete(page.getPreview().getStorageLocation());
            });
            this.solrService.delete(document);
            this.documentListeners.forEach(documentListener -> documentListener.deleted(document));
        });


        this.binaryService.delete(storedBinary);
    }

    private void handleMove(File file, InputType type, Binary storedBinary) {
        log.info("Found already known binary for [{}] with hash [{}]. Probably moved?", file, storedBinary.getHash());
        if (file.getAbsolutePath().equals(storedBinary.getStorageLocation())) {
            log.info("file path did not change");
        } else {
            log.info("file for binary [{}] got moved to [{}]", storedBinary.getId(), file.getAbsolutePath());
            this.binaryService.update(storedBinary.withStorageLocation(file.getAbsolutePath()));

            Document associatedDocument = documentService.getByBinary(storedBinary.getId());
            if (type == InputType.DOCUMENT && associatedDocument instanceof TaskDocument) {
                log.info("Document [{}] will be switched to Task", associatedDocument.getId());
                Document newDocument = new Document(associatedDocument.getId(), associatedDocument.getCreatedAt(), associatedDocument.getTitle(), associatedDocument.getDescription(), associatedDocument.getFile(), associatedDocument.getPages(), associatedDocument.getTags());
                this.documentService.delete(associatedDocument);
                this.solrService.delete(associatedDocument);
                this.documentService.store(newDocument);
                this.solrService.index(newDocument);
            } else if (type == InputType.TASK && !(associatedDocument instanceof TaskDocument)) {
                log.info("Document [{}] will be switched to Task", associatedDocument.getId());
                TaskDocument taskDocument = new TaskDocument(associatedDocument.getId(), associatedDocument.getCreatedAt(), associatedDocument.getTitle(), associatedDocument.getDescription(), associatedDocument.getFile(), State.OPEN, associatedDocument.getPages(), associatedDocument.getCreatedAt().plusDays(defaultDuePeriod), null, associatedDocument.getTags());
                this.documentService.delete(associatedDocument);
                this.solrService.delete(associatedDocument);
                this.documentService.store(taskDocument);
                this.solrService.index(taskDocument);
            }
        }
    }

    private String loadMimeType(File path) {
        try {
            MagicMatch magicMatch = Magic.getMagicMatch(path, true, false);
            return magicMatch.getMimeType();
        } catch (MagicParseException | MagicMatchNotFoundException | MagicException e) {
            log.warn("could not extract mimeType of [{}], will assume octet-stream", path);
            return "application/octet-stream";
        }
    }
}
