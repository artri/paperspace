package com.dedicatedcode.paperspace.model;

public interface DocumentListener {
    void changed(Document oldVersion, Document newVersion);

    void created(Document taskDocument);

    void deleted(Document document);
}
