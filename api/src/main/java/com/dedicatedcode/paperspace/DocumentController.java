package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.*;
import com.dedicatedcode.paperspace.search.SolrService;
import com.dedicatedcode.paperspace.web.DocumentResponse;
import com.dedicatedcode.paperspace.web.TaskDocumentResponse;
import com.dedicatedcode.paperspace.web.UnknownPageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
public class DocumentController {

    private final DocumentService documentService;
    private final BinaryService binaryService;
    private final SolrService solrService;
    private final int defaultTaskDuePeriod;
    private final List<TaskDocumentListener> taskListeners;
    private final List<DocumentListener> documentListeners;

    @Autowired
    public DocumentController(DocumentService documentService, BinaryService binaryService, SolrService solrService, @Value("${task.defaultDuePeriod}") int defaultTaskDuePeriod, List<TaskDocumentListener> taskListeners, List<DocumentListener> documentListeners) {
        this.documentService = documentService;
        this.binaryService = binaryService;
        this.solrService = solrService;
        this.defaultTaskDuePeriod = defaultTaskDuePeriod;
        this.taskListeners = taskListeners;
        this.documentListeners = documentListeners;
    }

    @PostMapping("/document")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse createNewDocument(@RequestBody DocumentUpload document) {
        Binary documentBinary = verifyData(document);
        Document newDocument = new Document(UUID.randomUUID(), LocalDateTime.now(), document.getTitle(), null, documentBinary, null, Collections.emptyList());
        this.solrService.index(newDocument);
        for (DocumentListener documentListener : documentListeners) {
            documentListener.created(newDocument);
        }
        return new DocumentResponse(this.documentService.store(newDocument));
    }

    @PostMapping("/task/{id}/done")
    public DocumentResponse markDone(@PathVariable UUID id) {
        TaskDocument document = (TaskDocument) this.documentService.getDocument(id);
        if (document == null) {
            throw new UnknownPageException("unable to find document with id [" + id + "]");
        }
        TaskDocument updated = document.withState(State.DONE).withDoneAt(LocalDateTime.now());
        this.documentService.update(updated);
        for (TaskDocumentListener taskListener : taskListeners) {
            taskListener.changed(document,updated);
        }
        this.solrService.index(updated);
        return new TaskDocumentResponse(updated);
    }

    @PostMapping({"/document/{id}", "/task/{id}",})
    public DocumentResponse updateDocument(@PathVariable UUID id, @RequestParam(required = false) String title, @RequestParam(required = false) String description, @RequestParam(required = false) String dueDate) {
        Document document = this.documentService.getDocument(id);
        if (document == null) {
            throw new UnknownPageException("unable to find document with id [" + id + "]");
        }
        if (document instanceof TaskDocument) {
            LocalDateTime dueValue = !dueDate.equals("") ? LocalDate.parse(dueDate, DateTimeFormatter.ofPattern("dd.MM.yyyy")).atStartOfDay() : null;
            TaskDocument task = ((TaskDocument) document).withDueAt(dueValue).withTitle(title).withDescription(description);
            this.documentService.update(task);
            for (TaskDocumentListener taskListener : taskListeners) {
                taskListener.changed((TaskDocument) document, task);
            }
        } else {
            document = document.withTitle(title).withDescription(description);
            this.documentService.update(document);
            for (DocumentListener documentListener : documentListeners) {
                documentListener.changed(document);
            }
        }
        this.solrService.index(document);

        return new DocumentResponse(document);
    }

    @PostMapping("/task")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDocumentResponse createNewTask(@RequestBody DocumentUpload document) {
        Binary documentBinary = verifyData(document);
        LocalDateTime dueDate = LocalDateTime.now().withMinute(0).withSecond(0).withHour(12).plusDays(defaultTaskDuePeriod);
        TaskDocument newDocument = new TaskDocument(UUID.randomUUID(), LocalDateTime.now(), document.getTitle(), null, documentBinary, State.OPEN, null, Collections.emptyList(), dueDate, null);
        TaskDocument storedDocument = this.documentService.store(newDocument);
        this.solrService.index(storedDocument);
        for (TaskDocumentListener taskListener : taskListeners) {
            taskListener.created(storedDocument);
        }
        return new TaskDocumentResponse(storedDocument);
    }

    @PostMapping("/document/{documentId}/pages")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse addNewPage(@PathVariable UUID documentId, @RequestBody PageUpload pageUpload) {
        Document document = this.documentService.getDocument(documentId);
        Page page = verifyPageUpload(documentId, pageUpload, document);
        Document updatedDocument = document.addPage(page);
        this.documentService.update(updatedDocument);
        this.solrService.index(updatedDocument);
        return new DocumentResponse(updatedDocument);
    }

    @PostMapping("/reindex")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String reindex() {
        this.solrService.reindex();
        return "started";
    }

    @PostMapping("/task/{documentId}/pages")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDocumentResponse addNewPageToTask(@PathVariable UUID documentId, @RequestBody PageUpload pageUpload) {
        TaskDocument document = (TaskDocument) this.documentService.getDocument(documentId);
        Page page = verifyPageUpload(documentId, pageUpload, document);
        TaskDocument updatedDocument = document.addPage(page);
        this.documentService.update(updatedDocument);
        this.solrService.index(updatedDocument);
        return new TaskDocumentResponse(updatedDocument);
    }

    private Page verifyPageUpload(UUID documentId, PageUpload pageUpload, Document document) {
        if (document == null) {
            throw new IllegalArgumentException("Unable to find document with id [" + documentId + "].");
        }
        Binary pageBinary = this.binaryService.get(pageUpload.binaryId);
        if (pageBinary == null) {
            throw new IllegalArgumentException("Unable to find binary with id [" + pageUpload.binaryId + "].");
        }
        return new Page(UUID.randomUUID(), pageUpload.number, pageUpload.text, pageBinary);
    }

    private Binary verifyData(@RequestBody DocumentUpload document) {
        Binary documentBinary = this.binaryService.get(document.getBinaryId());
        if (documentBinary == null) {
            throw new IllegalArgumentException("binary with id [" + document.getBinaryId() + "] not found.");
        }
        if (StringUtils.isEmpty(document.getTitle())) {
            throw new IllegalArgumentException("title must not be empty.");
        }
        return documentBinary;
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

    static class PageUpload {
        private long number;
        private UUID binaryId;
        private String text;

        public long getNumber() {
            return number;
        }

        public void setNumber(long number) {
            this.number = number;
        }

        public UUID getBinaryId() {
            return binaryId;
        }

        public void setBinaryId(UUID binaryId) {
            this.binaryId = binaryId;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
