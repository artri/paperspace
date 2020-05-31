package com.dedicatedcode.paperspace.mail;

import com.dedicatedcode.paperspace.model.Identifiable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by Daniel Wasilew on 20.07.17
 * (c) 2017 Daniel Wasilew <daniel@dedicatedcode.com>
 */
public class Message extends Identifiable {

    private final MessageType messageType;
    private final MessageState messageState;
    private final String subject;
    private final String body;
    private final LocalDateTime sendAt;
    private final String recipient;
    private final List<MessageAttachment> messageAttachments;

    public Message(UUID id, MessageType messageType, MessageState messageState, String subject, String body, LocalDateTime sendAt, String recipient, List<MessageAttachment> messageAttachments) {
        super(id);
        this.messageType = messageType;
        this.messageState = messageState;
        this.subject = subject;
        this.body = body;
        this.sendAt = sendAt;
        this.recipient = recipient;
        this.messageAttachments = Collections.unmodifiableList(messageAttachments);
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public MessageState getMessageState() {
        return messageState;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getSendAt() {
        return sendAt;
    }

    public Message withState(MessageState messageState) {
        return new Message(getId(), messageType, messageState, subject, body, sendAt, recipient, messageAttachments);
    }

    public Message withSendAt(LocalDateTime sendAt) {
        return new Message(getId(), messageType, messageState, subject, body, sendAt, recipient, messageAttachments);
    }

    public Message withRecipient(String recipient) {
        return new Message(getId(), messageType, messageState, subject, body, sendAt, recipient, messageAttachments);
    }

    public String getRecipient() {
        return recipient;
    }

    public List<MessageAttachment> getAttachments() {
        return this.messageAttachments;
    }
}
