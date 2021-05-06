package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.Document;
import com.dedicatedcode.paperspace.search.SolrService;
import com.dedicatedcode.paperspace.web.PageEditModel;
import com.dedicatedcode.paperspace.web.PageResponse;
import com.dedicatedcode.paperspace.web.UnknownPageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class DocumentEditController {

    private final DocumentService documentService;
    private final SolrService solrService;
    private final List<ModificationService> modificationServices;

    @Autowired
    public DocumentEditController(DocumentService documentService, SolrService solrService, List<ModificationService> modificationServices) {
        this.documentService = documentService;
        this.solrService = solrService;
        this.modificationServices = modificationServices;
    }

    @GetMapping("/api/edit/{documentId}")
    public List<PageEditModel> loadPageModel(@PathVariable UUID documentId) {
        Document document = this.documentService.getDocument(documentId);
        if (document == null) {
            throw new UnknownPageException("Unable to find document with id [" + documentId + "]");
        }
        return createPageEditModel(document);
    }

    private List<PageEditModel> createPageEditModel(Document document) {
        return document.getPages().stream().map(page -> new PageEditModel(new PageResponse(page), Collections.emptyList())).collect(Collectors.toList());
    }

    @PostMapping("/api/edit/{documentId}")
    public List<PageEditModel> updateDocument(@PathVariable UUID documentId, @RequestBody List<PageEditModel> body) {
        Document document = this.documentService.getDocument(documentId);
        if (document == null) {
            throw new UnknownPageException("Unable to find document with id [" + documentId + "]");
        }
        Binary binary = document.getFile();

        Optional<ModificationService> modifier = modificationServices.stream().filter(modificationService -> modificationService.supportedFileFormats().contains(binary.getMimeType())).filter(ModificationService::isEnabled).findFirst();
        if (modifier.isPresent()) {
            modifier.get().modify(binary, createPageEditModel(document), body);
            this.solrService.delete(document);
        } else {
            throw new UnavailableModifierException("Unable to modify file with type [" + binary.getMimeType() + "]");
        }
        return Collections.emptyList();
    }

    @ExceptionHandler(UnavailableModifierException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public String handleUnavailableModifierException(UnavailableModifierException ex) {
        return ex.getMessage();
    }

}
