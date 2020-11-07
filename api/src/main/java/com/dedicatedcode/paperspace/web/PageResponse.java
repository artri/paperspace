package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.model.Page;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PageResponse {
    private final Page page;
    private final Map<String,String> links;

    PageResponse(Page page) {
        this.page = page;
        this.links = createLinks(page);
    }

    private Map<String, String> createLinks(Page page) {
        Map<String,String> links = new HashMap<>();
        links.put("preview", "/api/image/" + page.getPreview().getId());
        return links;
    }

    public Map<String, String> getLinks() {
        return links;
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

}
