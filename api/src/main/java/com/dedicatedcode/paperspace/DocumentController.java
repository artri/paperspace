package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.*;
import com.dedicatedcode.paperspace.search.SolrService;
import com.dedicatedcode.paperspace.web.DocumentResponse;
import com.dedicatedcode.paperspace.web.TaskDocumentResponse;
import com.dedicatedcode.paperspace.web.UnknownPageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentService documentService;
    private final BinaryService binaryService;
    private final SolrService solrService;
    private final StorageService storageService;
    private final List<DocumentListener> documentListeners;
    private final TagService tagService;

    @Autowired
    public DocumentController(DocumentService documentService, BinaryService binaryService, SolrService solrService, @Value("${task.defaultDuePeriod}") int defaultTaskDuePeriod, StorageService storageService, List<DocumentListener> documentListeners, TagService tagService) {
        this.documentService = documentService;
        this.binaryService = binaryService;
        this.solrService = solrService;
        this.storageService = storageService;
        this.documentListeners = documentListeners;
        this.tagService = tagService;
    }

    @PostMapping({"/api/document/{id}", "/api/task/{id}",})
    public DocumentResponse updateDocument(@PathVariable UUID id,
                                           @RequestParam String title,
                                           @RequestParam(required = false) String description,
                                           @RequestParam(required = false) String dueDate,
                                           @RequestParam(required = false, defaultValue = "") List<String> tags) {
        Document oldVersion = this.documentService.getDocument(id);
        Document newVersion;
        if (oldVersion == null) {
            throw new UnknownPageException("unable to find document with id [" + id + "]");
        }
        List<Tag> allKnownTags = this.tagService.getAll();
        List<Tag> assignedTags = tags.stream().map(name -> allKnownTags
                    .stream()
                    .filter(tag -> tag.getName().equals(name))
                    .findAny()
                    .orElseGet(() -> {
                        log.info("creating new tag with name [{}]", name);
                        Tag newTag = new Tag(UUID.randomUUID(), name);
                        this.tagService.store(newTag);
                        return newTag;
                    })).collect(Collectors.toList());

        if (oldVersion instanceof TaskDocument) {
            TaskDocument task = ((TaskDocument) oldVersion).withTitle(title).withDescription(description).withTags(assignedTags);
            if (task.getState() != State.DONE) {
                LocalDateTime dueValue = dueDate != null && !dueDate.equals("") ? LocalDate.parse(dueDate, DateTimeFormatter.ofPattern("MM/dd/yyyy")).atStartOfDay() : null;
                task = task.withDueAt(dueValue);
            }
            this.documentService.update(task);
            newVersion = task;
        } else {
            newVersion = oldVersion.withTitle(title).withDescription(description).withTags(assignedTags);
            this.documentService.update(newVersion);
        }
        this.solrService.index(newVersion);

        for (DocumentListener documentListener : documentListeners) {
            documentListener.changed(oldVersion, newVersion);
        }

        return new DocumentResponse(oldVersion);
    }

    @PostMapping("/api/task/{id}/done")
    public DocumentResponse markDone(@PathVariable UUID id) {
        TaskDocument document = (TaskDocument) this.documentService.getDocument(id);
        if (document == null) {
            throw new UnknownPageException("unable to find document with id [" + id + "]");
        }
        TaskDocument updated = document.withState(State.DONE).withDoneAt(LocalDateTime.now());
        this.documentService.update(updated);
        this.solrService.index(updated);
        for (DocumentListener documentListener : documentListeners) {
            documentListener.changed(document, updated);
        }
        return new TaskDocumentResponse(updated);
    }

    @DeleteMapping({"/api/document/{id}", "/api/task/{id}",})
    public DocumentResponse deleteDocument(@PathVariable UUID id) {
        Document document = this.documentService.getDocument(id);
        if (document == null) {
            throw new UnknownPageException("unable to find document with id [" + id + "]");
        }
        this.documentService.delete(document);

        for (DocumentListener documentListener : documentListeners) {
                documentListener.deleted(document);
        }

        this.binaryService.delete(document.getFile());
        document.getPages().forEach(page -> this.binaryService.delete(page.getPreview()));
        this.storageService.delete(document);
        this.solrService.delete(document);
        return new DocumentResponse(document);
    }

    @PostMapping("/api/reindex")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String reindex() {
        this.solrService.reindex();
        return "started";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handle(IllegalArgumentException ex) {
        return new ErrorResponse(ex.getMessage(), 400);
    }

    static class DocumentUpload {
        private String title;
        private UUID binaryId;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public UUID getBinaryId() {
            return binaryId;
        }

        public void setBinaryId(UUID binaryId) {
            this.binaryId = binaryId;
        }
    }

}
