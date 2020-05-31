package com.dedicatedcode.paperspace.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskDocument extends Document {
    private final State state;
    private final LocalDateTime dueAt;
    private final LocalDateTime doneAt;

    public TaskDocument(UUID id, LocalDateTime createdAt, String title, String description, Binary file, State state, String content, List<Page> pages, LocalDateTime dueAt, LocalDateTime doneAt) {
        super(id, createdAt, title, description, file, content, pages);
        this.state = state;
        this.dueAt = dueAt;
        this.doneAt = doneAt;
    }

    public TaskDocument(UUID id, LocalDateTime createdAt, String title, String description, Binary file, State state, List<Page> pages, LocalDateTime dueAt, LocalDateTime doneAt) {
        super(id, createdAt, title, description, file, pages);
        this.state = state;
        this.dueAt = dueAt;
        this.doneAt = doneAt;
    }

    public State getState() {
        return state;
    }


    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public LocalDateTime getDoneAt() {
        return doneAt;
    }

    public TaskDocument withState(State state) {
        return new TaskDocument(getId(), getCreatedAt(), getTitle(), getDescription(), getFile(), state, getContent(), getPages(), dueAt, doneAt);
    }

    public TaskDocument withDueAt(LocalDateTime dueAt) {
        return new TaskDocument(getId(), getCreatedAt(), getTitle(), getDescription(), getFile(), state, getContent(), getPages(), dueAt, doneAt);
    }

    public TaskDocument withDoneAt(LocalDateTime doneAt) {
        return new TaskDocument(getId(), getCreatedAt(), getTitle(), getDescription(), getFile(), state, getContent(), getPages(), dueAt, doneAt);
    }

    public TaskDocument withTitle(String title) {
        return new TaskDocument(getId(), getCreatedAt(), title, getDescription(), getFile(), state, getContent(), getPages(), dueAt, doneAt);
    }

    public TaskDocument withDescription(String description) {
        return new TaskDocument(getId(), getCreatedAt(), getTitle(), description, getFile(), state, getContent(), getPages(), dueAt, doneAt);
    }

    public TaskDocument addPage(Page page) {
        List<Page> currentPages = new ArrayList<>(getPages());
        currentPages.add(page);
        return new TaskDocument(getId(), getCreatedAt(), getTitle(), getDescription(), getFile(), state, currentPages, dueAt, doneAt);
    }

}
