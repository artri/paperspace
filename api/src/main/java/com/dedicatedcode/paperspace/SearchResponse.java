package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.web.DocumentResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SearchResponse {
    private final List<DocumentResponse> items;
    private final Map<String, Object> pagination;
    private final List<TagFacet> tags;
    private final List<FilterFacet> filters;

    public SearchResponse(List<DocumentResponse> items, Map<String, Object> pagination, List<TagFacet> tags, List<FilterFacet> filters) {
        this.items = items;
        this.pagination = pagination;
        this.tags = tags;
        this.filters = filters;
    }

    public List<DocumentResponse> getItems() {
        return items;
    }

    public Map<String, Object> getPagination() {
        return pagination;
    }

    public List<TagFacet> getTags() {
        return tags;
    }

    public List<FilterFacet> getFilters() {
        return filters;
    }

    public static class FilterFacet {
        private final String name;
        private final long count;
        private final boolean active;

        public FilterFacet(String name, long count, boolean active) {
            this.name = name;
            this.count = count;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public long getCount() {
            return count;
        }

        public boolean isActive() {
            return active;
        }
    }
    public static class TagFacet {
        private final UUID id;
        private final String name;
        private final long count;
        private final boolean active;

        public TagFacet(UUID id, String name, long count, boolean active) {
            this.id = id;
            this.name = name;
            this.count = count;
            this.active = active;
        }

        public UUID getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public long getCount() {
            return count;
        }

        public boolean isActive() {
            return active;
        }
    }
}
