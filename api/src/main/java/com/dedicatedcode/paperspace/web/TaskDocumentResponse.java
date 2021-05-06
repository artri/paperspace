package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.model.Document;
import com.dedicatedcode.paperspace.model.State;
import com.dedicatedcode.paperspace.model.TaskDocument;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class TaskDocumentResponse extends DocumentResponse {
    private final TaskDocument document;

    public TaskDocumentResponse(TaskDocument document) {
        this(document, null);
    }

    public TaskDocumentResponse(TaskDocument document, String previewText) {
        super(document, createLinkMap(document), previewText);
        this.document = document;
    }

    private static Map<String, String> createLinkMap(Document document) {
        HashMap<String, String> map = new HashMap<>();
        map.put("self", "/task/" + document.getId());
        map.put("edit", "/api/task/" + document.getId());
        map.put("editPages", "/task/edit/" + document.getId());
        map.put("pages", "/api/task/" + document.getId() + "/pages");
        map.put("download", "/api/task/" + document.getFile().getId());
        map.put("done", "/api/task/" + document.getId() + "/done");
        map.put("view", "/api/view/" + document.getFile().getId());
        map.put("preview", document.getPages().stream().findFirst().map(page -> "/api/image/" + page.getPreview().getId() + "?width=560").orElse(null));
        return map;
    }

    public String getType() {
        return "TASK";
    }

    public State getState() {
        return document.getState();
    }

    public boolean isDone() {
        return this.document.getState() == State.DONE;
    }

    public LocalDateTime getDueAt() {
        return document.getDueAt();
    }

    public LocalDateTime getDoneAt() {
        return document.getDoneAt();
    }
}
