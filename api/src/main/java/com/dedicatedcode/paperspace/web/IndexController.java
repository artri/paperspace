package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.DocumentService;
import com.dedicatedcode.paperspace.ModificationService;
import com.dedicatedcode.paperspace.TagService;
import com.dedicatedcode.paperspace.feeder.MergingFileEventHandler;
import com.dedicatedcode.paperspace.model.*;
import com.dedicatedcode.paperspace.search.SolrService;
import com.dedicatedcode.paperspace.search.SolrVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class IndexController {
    private final DocumentService documentService;
    private final List<DocumentListener> documentListeners;
    private final SolrService solrService;
    private final SolrVersionService solrVersionService;
    private final MergingFileEventHandler fileEventHandler;
    private final TagService tagService;
    private final String appHost;
    private final List<ModificationService> modificationServices;

    @Autowired
    public IndexController(DocumentService documentService, List<DocumentListener> documentListeners, SolrService solrService, SolrVersionService solrVersionService, MergingFileEventHandler fileEventHandler, TagService tagService, @Value("${app.host}") String appHost, List<ModificationService> modificationServices) {
        this.documentService = documentService;
        this.documentListeners = documentListeners;
        this.solrService = solrService;
        this.solrVersionService = solrVersionService;
        this.fileEventHandler = fileEventHandler;
        this.tagService = tagService;
        this.appHost = appHost;
        this.modificationServices = modificationServices;
    }

    @RequestMapping("/")
    public String loadIndexPage(ModelMap modelMap) {
        modelMap.addAttribute("tags", this.tagService.getAll().stream().sorted(Comparator.comparing(Tag::getName)).collect(Collectors.toList()));
        return "index";
    }

    @RequestMapping("/api/status.json")
    @ResponseBody
    public Status loadStatus() {
        return new Status(this.solrVersionService.needsReindexing(), fileEventHandler.getPendingChanges());
    }

    @RequestMapping({"/task/{id}", "/document/{id}"})
    public String loadDocumentPage(@PathVariable UUID id, ModelMap model) {
        updateDocumentModel(id, model);
        return "task";
    }

    @RequestMapping({"/task/edit/{id}", "/document/edit/{id}"})
    public String loadDocumentEditPage(@PathVariable UUID id, ModelMap model) {
        updateDocumentModel(id, model);
        return "edit";
    }

    private void updateDocumentModel(@PathVariable UUID id, ModelMap model) {
        Document document = this.documentService.getDocument(id);
        if (document == null) {
            throw new UnknownPageException("unable to find document with id [" + id + "]");
        }
        if (document instanceof TaskDocument) {
            model.addAttribute("document", new TaskDocumentResponse((TaskDocument) document));
        } else {
            model.addAttribute("document", new DocumentResponse(document));
        }
        model.addAttribute("editable", this.modificationServices.stream().anyMatch(ms -> ms.isEnabled() && ms.supportedFileFormats().contains(document.getFile().getMimeType())));
        model.addAttribute("appHost", appHost);
    }

    @GetMapping("/task/{id}/done")
    public String markDoneFromMail(@PathVariable UUID id) {
        TaskDocument document = (TaskDocument) this.documentService.getDocument(id);
        if (document == null) {
            throw new UnknownPageException("unable to find document with id [" + id + "]");
        }
        if (document.getState() != State.DONE) {
            TaskDocument updated = document.withState(State.DONE).withDoneAt(LocalDateTime.now());
            this.documentService.update(updated);
            for (DocumentListener documentListener : documentListeners) {
                documentListener.changed(document, updated);
            }
            this.solrService.index(updated);
        }
        return "redirect:/task/" + id + "?marked_done";
    }

    @ExceptionHandler(UnknownPageException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public String handleNotFound(UnknownPageException ex) {
        return ex.getMessage();
    }

    private static class Status {
        private final String data;
        private final int pendingChanges;

        private Status(SolrVersionService.Status data, int pendingChanges) {
            this.data = data.name();
            this.pendingChanges = pendingChanges;
        }

        public String getData() {
            return data;
        }

        public int getPendingChanges() {
            return pendingChanges;
        }
    }
}
