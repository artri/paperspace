package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.DocumentService;
import com.dedicatedcode.paperspace.model.Document;
import com.dedicatedcode.paperspace.model.State;
import com.dedicatedcode.paperspace.model.TaskDocument;
import com.dedicatedcode.paperspace.model.TaskDocumentListener;
import com.dedicatedcode.paperspace.search.SolrService;
import com.dedicatedcode.paperspace.search.SolrVersionService;
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
    private final List<TaskDocumentListener> taskListeners;
    private final SolrService solrService;
    private final SolrVersionService solrVersionService;
    private final String appHost;

    @Autowired
    public IndexController(DocumentService documentService, List<TaskDocumentListener> taskListeners, SolrService solrService, SolrVersionService solrVersionService, @Value("${app.host}") String appHost) {
        this.documentService = documentService;
        this.taskListeners = taskListeners;
        this.solrService = solrService;
        this.solrVersionService = solrVersionService;
        this.appHost = appHost;
    }

    @RequestMapping("/")
    public String loadIndexPage() {
        return "index";
    }

    @RequestMapping("/status") 
    @ResponseBody
    public Status loadStatus() {
        return new Status(this.solrVersionService.needsReindexing());
    }

    @RequestMapping({"/task/{id}", "/document/{id}"})
    public String loadDocumentPage(@PathVariable UUID id, ModelMap model) {
        Document document = this.documentService.getDocument(id);
        if (document == null) {
            throw new UnknownPageException("unable to find document with id [" + id + "]");
        }
        if (document instanceof TaskDocument) {
            model.addAttribute("document", new TaskDocumentResponse((TaskDocument) document));
        } else {
            model.addAttribute("document", new DocumentResponse(document));
        }
        model.addAttribute("appHost", appHost);
        return "task";
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
            for (TaskDocumentListener taskListener : taskListeners) {
                taskListener.changed(document, updated);
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

        private Status(SolrVersionService.Status data) {
            this.data = data.name();
        }

        public String getData() {
            return data;
        }
    }
}
