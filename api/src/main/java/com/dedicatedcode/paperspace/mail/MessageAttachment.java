package com.dedicatedcode.paperspace.mail;

import com.dedicatedcode.paperspace.model.Binary;

import java.util.UUID;

public class MessageAttachment {
    public static MessageAttachment create(Binary invoice) {
        return new MessageAttachment(AttachmentType.BINARY, invoice.getId());
    }

    private final AttachmentType type;
    private final UUID id;

    MessageAttachment(AttachmentType type, UUID id) {
        this.type = type;
        this.id = id;
    }

    public AttachmentType getType() {
        return type;
    }

    public UUID getId() {
        return id;
    }

    enum AttachmentType {
        BINARY
    }
}
