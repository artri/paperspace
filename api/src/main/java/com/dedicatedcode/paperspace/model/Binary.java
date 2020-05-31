package com.dedicatedcode.paperspace.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Binary extends Identifiable{
    private final LocalDateTime createdAt;
    private final String originalFileName;
    private final String mimeType;
    private final long length;

    public Binary(UUID id, LocalDateTime createdAt, String originalFileName, String mimeType, long length) {
        super(id);
        this.createdAt = createdAt;
        this.originalFileName = originalFileName;
        this.mimeType = mimeType;
        this.length = length;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getLength() {
        return length;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }
}
