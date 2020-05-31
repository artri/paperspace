package com.dedicatedcode.paperspace.model;

import java.util.UUID;

public class Page extends Identifiable{
    private final long number;
    private final String content;
    private final Binary preview;

    public Page(UUID id, long number, String content, Binary preview) {
        super(id);
        this.number = number;
        this.content = content;
        this.preview = preview;
    }

    public long getNumber() {
        return number;
    }

    public String getContent() {
        return content;
    }

    public Binary getPreview() {
        return preview;
    }
}
