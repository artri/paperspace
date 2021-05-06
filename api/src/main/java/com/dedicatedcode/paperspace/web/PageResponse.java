package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.model.Page;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PageResponse {
    private final UUID id;
    private final long number;
    private final String content;
    private final Map<String, String> links;

    public PageResponse(UUID id, long number, String content, Map<String, String> links) {
        this.id = id;
        this.number = number;
        this.content = content;
        this.links = links;
    }

    public PageResponse(Page page) {
        this.id = page.getId();
        this.number = page.getNumber();
        this.content = page.getContent();
        this.links = createLinks(page);
    }

    private Map<String, String> createLinks(Page page) {
        Map<String, String> links = new HashMap<>();
        if (page.getPreview() != null) {
            links.put("preview", "/api/image/" + page.getPreview().getId());
        }
        return links;
    }

    public UUID getId() {
        return id;
    }

    public long getNumber() {
        return number;
    }

    public String getContent() {
        return content;
    }

    public Map<String, String> getLinks() {
        return links;
    }
}
