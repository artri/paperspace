package com.dedicatedcode.paperspace.model;

public interface TaskDocumentListener {
    void changed(TaskDocument oldVersion, TaskDocument newVersion);
    void created(TaskDocument taskDocument);
    void deleted(TaskDocument document);
}
