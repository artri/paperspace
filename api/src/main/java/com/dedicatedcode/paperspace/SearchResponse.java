package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.web.DocumentResponse;

import java.util.List;
import java.util.Map;

public class SearchResponse {
    private final List<DocumentResponse> items;
    private final long page;
    private final long totalPages;
    private final long results;
    private final Map<String, Object> pagination;

    public SearchResponse(List<DocumentResponse> items, long page, long totalPages, long results, Map<String, Object> pagination) {
        this.items = items;
        this.page = page;
        this.totalPages = totalPages;
        this.results = results;
        this.pagination = pagination;
    }

    public List<DocumentResponse> getItems() {
        return items;
    }

    public long getPage() {
        return page;
    }

    public long getTotalPages() {
        return totalPages;
    }

    public long getResults() {
        return results;
    }

    public Map<String, Object> getPagination() {
        return pagination;
    }
}
