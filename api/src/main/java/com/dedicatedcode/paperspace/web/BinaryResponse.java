package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.model.Binary;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class BinaryResponse {
    private final Binary binary;
    private final Map<String,String> links;

    public BinaryResponse(Binary binary) {
        this.binary = binary;
        this.links = Collections.singletonMap("download", "/download/" + binary.getId());
    }

    public UUID getId() {
        return binary.getId();
    }

    public String getMimeType() {
        return binary.getMimeType();
    }

    public long getLength() {
        return binary.getLength();
    }

    public LocalDateTime getCreatedAt() {
        return binary.getCreatedAt();
    }

    public String getStorageLocation() {
        return binary.getStorageLocation();
    }

    public String getFilename() {
        return this.binary.getStorageLocation().substring(this.binary.getStorageLocation().lastIndexOf("/") + 1);
    }

    public Map<String, String> getLinks() {
        return links;
    }
}
