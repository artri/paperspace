package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.DocumentService;
import com.dedicatedcode.paperspace.ModificationService;
import com.dedicatedcode.paperspace.model.Document;
import com.dedicatedcode.paperspace.model.DocumentListener;
import com.dedicatedcode.paperspace.model.State;
import com.dedicatedcode.paperspace.model.TaskDocument;
import com.dedicatedcode.paperspace.search.SolrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
public class IndexController {
    private final DocumentService documentService;
    private final List<DocumentListener> documentListeners;
    private final SolrService solrService;
    private final String appHost;
    private final List<ModificationService> modificationServices;

    @Autowired
    public IndexController(DocumentService documentService, List<DocumentListener> documentListeners, SolrService solrService, @Value("${app.host}") String appHost, List<ModificationService> modificationServices) {
        this.documentService = documentService;
        this.documentListeners = documentListeners;
        this.solrService = solrService;
        this.appHost = appHost;
        this.modificationServices = modificationServices;
    }

    @RequestMapping("/")
    public String loadIndexPage() {
        return "index";
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

}
