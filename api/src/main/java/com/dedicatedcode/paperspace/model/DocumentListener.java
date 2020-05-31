package com.dedicatedcode.paperspace.model;

public interface DocumentListener {
    void changed(Document taskDocument);
    void created(Document taskDocument);
}
