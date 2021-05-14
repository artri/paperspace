package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.Document;
import com.dedicatedcode.paperspace.model.Tag;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.unbescape.html.HtmlEscape;
import org.unbescape.html.HtmlEscapeType;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class DocumentResponse {
    private final Document document;
    private final Map<String, String> links;
    private final String previewText;
    private final List<PageResponse> pages;
    private final List<Tag> tags;

    public DocumentResponse(Document document) {
        this(document, null);
    }

    public DocumentResponse(Document document, String previewText) {
        this(document, createLinkMap(document), previewText);
    }

    private static Map<String, String> createLinkMap(Document document) {
        HashMap<String, String> map = new HashMap<>();
        map.put("self", "/document/" + document.getId());
        map.put("edit", "/api/document/" + document.getId());
        map.put("editPages", "/document/edit/" + document.getId());
        map.put("pages", "/api/edit/" + document.getId());
        map.put("download", "/api/download/" + document.getFile().getId());
        map.put("view", "/api/view/" + document.getFile().getId());
        map.put("preview", document.getPages().stream().findFirst().map(page -> "/api/image/" + page.getPreview().getId() + "?width=560").orElse(null));
        return map;
    }

    DocumentResponse(Document document, Map<String, String> links, String previewText) {
        this.document = document;
        this.links = links;
        this.previewText = createPreviewText(document, previewText);
        this.pages = document.getPages().stream().map(PageResponse::new).collect(Collectors.toList());
        this.tags = document.getTags();
    }

    private String createPreviewText(Document document, String previewText) {
        if (previewText == null) {
            return HtmlUtils.htmlEscape(createFromContent(document));
        } else {
            return HtmlUtils.htmlEscape(previewText)
                    .replaceAll("&lt;em&gt;", "<em>")
                    .replaceAll("&lt;/em&gt;", "</em>");
        }

    }

    private String createFromContent(Document document) {
        if (document == null || StringUtils.isEmpty(document.getContent())) {
            return "";
        } else {
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

    public List<Tag> getTags() {
        return tags;
    }
}
