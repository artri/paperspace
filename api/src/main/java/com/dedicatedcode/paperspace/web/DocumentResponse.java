package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.Document;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class DocumentResponse {
    private final Document document;
    private final Map<String, String> links;
    private final String previewText;
    private final List<PageResponse> pages;

    public DocumentResponse(Document document) {
        this(document, null);
    }
    public DocumentResponse(Document document, String previewText) {
        this(document, new Links("/document/", document), previewText, Collections.emptyMap());
    }

    DocumentResponse(Document document, Links links, String previewText, Map<String, String> additionalLinks) {
        this.document = document;
        this.links = links.toMap();
        if (previewText == null) {
            this.previewText = createFromContent(document);
        } else {
            this.previewText = previewText;
        }
        this.links.putAll(additionalLinks);
        this.pages = document.getPages().stream().map(PageResponse::new).collect(Collectors.toList());
    }

    private String createFromContent(Document document) {
        if (document == null || StringUtils.isEmpty(document.getContent())){
            return null;
        }
        else {
            String content = document.getContent();
            content = content.substring(0, Math.min(content.length(), 150));
            String[] lines = content.split("\\n");
            return Arrays.stream(lines).limit(5).collect(Collectors.joining("\n"));
        }
    }

    public UUID getId() {
        return document.getId();
    }

    public String getTitle() {
        return document.getTitle();
    }

    public String getType() {
        return "DOCUMENT";
    }

    public String getPreviewText() {
        return previewText;
    }

    public String getDescription() {
        return document.getDescription();
    }

    public Binary getFile() {
        return document.getFile();
    }

    public LocalDateTime getCreatedAt() {
        return document.getCreatedAt();
    }

    public String getContent() {
        return document.getContent();
    }

    public List<PageResponse> getPages() {
        return pages;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public static class Links {
        private final String self;
        private final String pages;
        private final String view;
        private final String download;

        Links(String base, Document document) {
            this.self = base + document.getId();
            this.pages = base + document.getId() + "/pages";
            this.download = "/download/" + document.getFile().getId();
            this.view = "/view/" + document.getFile().getId();
        }

        public String getSelf() {
            return self;
        }

        public String getPages() {
            return pages;
        }

        public String getView() {
            return view;
        }

        public String getDownload() {
            return download;
        }

        public Map<String, String> toMap() {
            HashMap<String, String> map = new HashMap<>();
            map.put("self", self);
            map.put("pages", pages);
            map.put("download", download);
            map.put("view", view);
            return map;
        }
    }

}
