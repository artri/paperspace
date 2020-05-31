package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.model.Page;

import java.util.UUID;

public class PageResponse {
    private final Page page;
    private final BinaryResponse preview;

    PageResponse(Page page) {
        this.page = page;
        this.preview = new BinaryResponse(page.getPreview());
    }

    public UUID getId() {
        return page.getId();
    }

    public long getNumber() {
        return page.getNumber();
    }

    public String getContent() {
        return page.getContent();
    }

    public BinaryResponse getPreview() {
        return this.preview;
    }
}
