package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.model.Binary;

import java.time.LocalDateTime;
import java.util.UUID;

public class BinaryResponse {
    private final Binary binary;
    private final Links links;

    public BinaryResponse(Binary binary) {
        this.binary = binary;
        this.links = new Links("/download/" + binary.getId());
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

    public String getOriginalFileName() {
        return binary.getOriginalFileName();
    }

    public Links getLinks() {
        return links;
    }

    private static class Links {
        private final String download;

        private Links(String download) {
            this.download = download;
        }

        public String getDownload() {
            return download;
        }
    }
}
