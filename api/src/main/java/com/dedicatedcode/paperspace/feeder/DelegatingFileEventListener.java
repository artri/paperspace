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
import java.util.stream.Collectors;


@Service
public class DelegatingFileEventListener implements FileEventListener {
    private static final Logger log = LoggerFactory.getLogger(DelegatingFileEventListener.class);
    private final BinaryService binaryService;
    private final DocumentService documentService;
    private final StorageService storageService;
    private final SolrService solrService;
    private final List<OcrService> ocrServices;
    private final int defaultDuePeriod;
    private final List<DocumentListener> documentListeners;

    public DelegatingFileEventListener(BinaryService binaryService,
                                       DocumentService documentService,
                                       StorageService storageService,
                                       SolrService solrService,
                                       List<OcrService> ocrServices,
                                       @Value("${task.defaultDuePeriod}") int defaultDuePeriod, List<DocumentListener> documentListeners) {
        this.binaryService = binaryService;
        this.documentService = documentService;
        this.storageService = storageService;
        this.solrService = solrService;
        this.ocrServices = ocrServices;
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
        if (!file.exists()) {
            log.warn("File [{}] got deleted before we could process it", file);
            return;
        }
        String md5 = getHashFromFile(file);
        try {
            if (this.binaryService.getByHash(md5) != null) {
                log.warn("Duplicate [{}] detected!", file);
                if (this.binaryService.getAll().stream().noneMatch(binary -> binary.getStorageLocation().equals(file.getAbsolutePath()))) {
                    this.binaryService.store(new Binary(UUID.randomUUID(), LocalDateTime.now(), file.getAbsolutePath(), md5, loadMimeType(file), file.length(), OCRState.DUPLICATE));
                }
                return;
            }

            List<Page> pages = doOcr(file);
            LocalDateTime now = LocalDateTime.now();
            Binary documentBinary = new Binary(UUID.randomUUID(), now, file.getAbsolutePath(), md5, loadMimeType(file), file.length(), OCRState.PROCESSED);
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
            this.binaryService.store(new Binary(UUID.randomUUID(), LocalDateTime.now(), file.getAbsolutePath(), md5, loadMimeType(file), file.length(), OCRState.FAILED));
            log.error("processing of [{}] failed with:", file, e);
        }
    }

    private List<Page> doOcr(File file) throws OcrException {
        String mimeType = FileTypes.loadMimeType(file);
        log.debug("Searching for a an ocr handler for [{}] with mimeType [{}]", file, mimeType);
        for (OcrService ocrService : ocrServices) {
            if (ocrService.supportedFileFormats().contains(mimeType)) {
                return ocrService.doOcr(file);
            }
        }
        throw new OcrException("Unhandled file format [" + mimeType + "] detected for [" + file + "], supported formats are [" + ocrServices.stream().flatMap(service -> service.supportedFileFormats().stream()).collect(Collectors.joining(",")) + "]");
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
        Binary binary = this.binaryService.getByPath(inputFile.getAbsolutePath());
        if (binary == null && inputFile.exists()) {
            try (InputStream is = new FileInputStream(inputFile)) {
                binary = this.binaryService.getByHash(DigestUtils.md5DigestAsHex(is));
            } catch (IOException e) {
                log.error("could not calculate md5 sum of [{}]", inputFile);
                return Optional.empty();
            }
        }

        if (binary != null) {
            File knownFile = new File(binary.getStorageLocation());
            if (!knownFile.getName().equals(inputFile.getName())) {
                binary = null;
            }
        }

        return Optional.ofNullable(binary);
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
            List<Page> pages = doOcr(file);
            Document updatedDocument = associatedDocument.withPages(pages);
            pages.forEach(page -> this.binaryService.store(page.getPreview()));
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
