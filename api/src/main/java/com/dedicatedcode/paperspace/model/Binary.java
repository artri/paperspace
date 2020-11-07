package com.dedicatedcode.paperspace.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Binary extends Identifiable{
    private final LocalDateTime createdAt;
    private final String storageLocation;
    private final String hash;
    private final String mimeType;
    private final long length;

    public Binary(UUID id, LocalDateTime createdAt, String storageLocation, String hash, String mimeType, long length) {
        super(id);
        this.createdAt = createdAt;
        this.storageLocation = storageLocation;
        this.hash = hash;
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

    public String getStorageLocation() {
        return storageLocation;
    }

    public String getHash() {
        return hash;
    }

    public Binary withStorageLocation(String storageLocation) {
        return new Binary(getId(), createdAt, storageLocation, hash, mimeType, length);
    }

    public Binary withHash(String hash) {
        return new Binary(getId(), createdAt, storageLocation, hash, mimeType, length);
    }
}
