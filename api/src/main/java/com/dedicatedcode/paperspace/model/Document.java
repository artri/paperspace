package com.dedicatedcode.paperspace.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Document extends Identifiable{
    private final String title;
    private final LocalDateTime createdAt;
    private final String description;
    private final Binary file;
    private final String content;
    private final List<Page> pages;
    private final List<Tag> tags;

    public Document(UUID id, LocalDateTime createdAt, String title, String description, Binary file, String content, List<Page> pages, List<Tag> tags) {
        super(id);
        this.title = title;
        this.createdAt = createdAt;
        this.description = description;
        this.file = file;
        this.content = content;
        this.pages = Collections.unmodifiableList(pages);
        this.tags = tags;
    }

    public Document(UUID id, LocalDateTime createdAt, String title, String description, Binary file, List<Page> pages, List<Tag> tags) {
        this(id, createdAt, title, description, file, pages.stream().map(Page::getContent).collect(Collectors.joining("\n")), pages, Collections.emptyList());
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Binary getFile() {
        return file;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getContent() {
        return content;
    }

    public List<Page> getPages() {
        return pages;
    }

    public List<Tag> getTags() {
        return this.tags;
    }

    public Document withPages(List<Page> pages) {
        return new Document(getId(), getCreatedAt(), title, getDescription(), file, getContent(), pages, tags);
    }

    public Document withTitle(String title) {
        return new Document(getId(), getCreatedAt(), title, getDescription(), file, getContent(), getPages(), tags);
    }

    public Document withDescription(String description) {
        return new Document(getId(), getCreatedAt(), getTitle(), description, file, getContent(), getPages(), tags);
    }

    public Document withTags(List<Tag> tags) {
        return new Document(getId(), getCreatedAt(), getTitle(), description, file, getContent(), getPages(), tags);
    }
}