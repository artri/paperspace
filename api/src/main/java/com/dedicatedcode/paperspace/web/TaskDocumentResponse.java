package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.model.State;
import com.dedicatedcode.paperspace.model.TaskDocument;

import java.time.LocalDateTime;
import java.util.Collections;

public class TaskDocumentResponse extends DocumentResponse {
    private final TaskDocument document;

    public TaskDocumentResponse(TaskDocument document) {
        this(document, null);
    }

    public TaskDocumentResponse(TaskDocument document, String previewText) {
        super(document, new DocumentResponse.Links("/task/", document), previewText, Collections.singletonMap("done", "/task/" + document.getId() + "/done"));
        this.document = document;
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
