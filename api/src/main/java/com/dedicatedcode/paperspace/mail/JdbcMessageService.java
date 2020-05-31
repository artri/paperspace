package com.dedicatedcode.paperspace.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static com.dedicatedcode.paperspace.SQLHelper.nullableString;

/**
 * Created by Daniel Wasilew on 20.07.17
 * (c) 2017 Daniel Wasilew <daniel@dedicatedcode.com>
 */
@Service
public class JdbcMessageService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcMessageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void storeMessage(Message message, String identifier) {
        String messageId = message.getId().toString();
        this.jdbcTemplate.update("INSERT INTO outgoing_messages(id, type, identifier, subject_line, body, send_at, message_state, recipient) VALUES (?,?,?,?,?,?,?,?)",
                messageId,
                message.getMessageType().name(),
                identifier,
                message.getSubject(),
                message.getBody(),
                Timestamp.valueOf(message.getSendAt()),
                message.getMessageState().name(),
                message.getRecipient());

        int position = 0;
        for (MessageAttachment attachment : message.getAttachments()) {
            this.jdbcTemplate.update("INSERT INTO outgoing_messages_attachments(message_id, position, type, source_id)" +
                            "VALUES(?,?,?,?)",
                    messageId,
                    position++,
                    attachment.getType().name(),
                    attachment.getId().toString());
        }
    }

    void deleteMessage(Message message) {
        String messageId = message.getId().toString();
        this.jdbcTemplate.update("DELETE FROM outgoing_messages WHERE id = ?", messageId);
        this.jdbcTemplate.update("DELETE FROM outgoing_messages_attachments WHERE message_id = ?", messageId);
    }

    void moveToProcessedMessages(Message message) {
        deleteMessage(message);
        String messageId = message.getId().toString();
        this.jdbcTemplate.update("INSERT INTO processed_messages(id, type, recipient, subject_line, body, sent_at, message_state) VALUES (?,?,?,?,?,?,?)",
                messageId,
                message.getMessageType().name(),
                message.getRecipient(),
                message.getSubject(),
                message.getBody(),
                Timestamp.valueOf(message.getSendAt()),
                message.getMessageState().name());

        int position = 0;
        for (MessageAttachment attachment : message.getAttachments()) {
            this.jdbcTemplate.update("INSERT INTO processed_messages_attachments(message_id, position, type, source_id)" +
                            "VALUES(?,?,?,?)",
                    messageId,
                    position++,
                    attachment.getType().name(),
                    attachment.getId().toString());
        }
    }

    public Message getScheduledMessageBy(UUID id) {
        Map<UUID, List<MessageAttachment>> attachments = new HashMap<>();
        this.jdbcTemplate.query(
                "SELECT message_id, ma.type, source_id FROM outgoing_messages_attachments ma WHERE ma.message_id = ? ORDER BY ma.message_id, ma.position", (RowMapper<Void>) (rs, i) -> {
                    List<MessageAttachment> currentAttachments = attachments.computeIfAbsent(UUID.fromString(rs.getString("message_id")), uuid -> new ArrayList<>());
                    currentAttachments.add(new MessageAttachment(MessageAttachment.AttachmentType.valueOf(rs.getString("type")), UUID.fromString(rs.getString("source_id"))));
                    return null;
                }, id.toString());
        List<Message> query = this.jdbcTemplate.query("SELECT * FROM outgoing_messages WHERE id = ?", new ScheduledMessageRowMapper(attachments), id.toString());
        if (query.isEmpty()) {
            return null;
        } else {
            return query.get(0);
        }
    }

    public Message getProcessedMessageBy(UUID id) {
        Map<UUID, List<MessageAttachment>> attachments = new HashMap<>();
        this.jdbcTemplate.query(
                "SELECT message_id, ma.type, source_id FROM processed_messages_attachments ma WHERE ma.message_id = ? ORDER BY ma.message_id, ma.position", (RowMapper<Void>) (rs, i) -> {
                    List<MessageAttachment> currentAttachments = attachments.computeIfAbsent(UUID.fromString(rs.getString("message_id")), uuid -> new ArrayList<>());
                    currentAttachments.add(new MessageAttachment(MessageAttachment.AttachmentType.valueOf(rs.getString("type")), UUID.fromString(rs.getString("source_id"))));
                    return null;
                }, id.toString());
        List<Message> query = this.jdbcTemplate.query("SELECT * FROM processed_messages WHERE id = ?", new ProcessedMessageRowMapper(attachments), id.toString());
        if (query.isEmpty()) {
            return null;
        } else {
            return query.get(0);
        }
    }

    public List<Message> getScheduledMessageBy(String messageIdentifier) {
        Map<UUID, List<MessageAttachment>> attachments = new HashMap<>();
        this.jdbcTemplate.query(
                "SELECT message_id, ma.type, source_id FROM outgoing_messages_attachments ma LEFT JOIN outgoing_messages pm ON pm.id = ma.message_id WHERE pm.identifier = ? ORDER BY message_id, ma.position", (RowMapper<Void>) (rs, i) -> {
                    List<MessageAttachment> currentAttachments = attachments.computeIfAbsent(UUID.fromString(rs.getString("message_id")), uuid -> new ArrayList<>());
                    currentAttachments.add(new MessageAttachment(MessageAttachment.AttachmentType.valueOf(rs.getString("type")), UUID.fromString(rs.getString("source_id"))));
                    return null;
                }, messageIdentifier);
        return this.jdbcTemplate.query("SELECT * FROM outgoing_messages WHERE identifier = ?", new ScheduledMessageRowMapper(attachments), messageIdentifier);
    }

    public List<Message> getAllScheduled() {
        Map<UUID, List<MessageAttachment>> attachments = new HashMap<>();
        this.jdbcTemplate.query(
                "SELECT message_id, ma.type, source_id FROM outgoing_messages_attachments ma LEFT JOIN outgoing_messages pm ON pm.id = ma.message_id WHERE message_state='SCHEDULED' ORDER BY message_id, ma.position", (RowMapper<Void>) (rs, i) -> {
                    List<MessageAttachment> currentAttachments = attachments.computeIfAbsent(UUID.fromString(rs.getString("message_id")), uuid -> new ArrayList<>());
                    currentAttachments.add(new MessageAttachment(MessageAttachment.AttachmentType.valueOf(rs.getString("type")), UUID.fromString(rs.getString("source_id"))));
                    return null;
                });
        return this.jdbcTemplate.query("SELECT * FROM outgoing_messages WHERE message_state = 'SCHEDULED'", new ScheduledMessageRowMapper(attachments));
    }


    private static final class ScheduledMessageRowMapper implements RowMapper<Message> {
        private final Map<UUID, List<MessageAttachment>> attachments;

        ScheduledMessageRowMapper(Map<UUID, List<MessageAttachment>> attachments) {
            this.attachments = attachments;
        }

        @Override
        public Message mapRow(ResultSet rs, int rowNum) throws SQLException {
            UUID id = UUID.fromString(rs.getString("id"));
            MessageType messageType = MessageType.valueOf(rs.getString("type"));
            String subject = rs.getString("subject_line");
            String body = rs.getString("body");
            String recipient = nullableString(rs, "recipient");
            LocalDateTime sendAt = rs.getTimestamp("send_at").toLocalDateTime();
            MessageState messageState = MessageState.valueOf(rs.getString("message_state"));
            return new Message(id, messageType, messageState, subject, body, sendAt, recipient, attachments.getOrDefault(id, Collections.emptyList()));
        }
    }

    private static final class ProcessedMessageRowMapper implements RowMapper<Message> {

        private Map<UUID, List<MessageAttachment>> attachments;

        ProcessedMessageRowMapper(Map<UUID, List<MessageAttachment>> attachments) {
            this.attachments = attachments;
        }

        @Override
        public Message mapRow(ResultSet rs, int rowNum) throws SQLException {
            UUID id = UUID.fromString(rs.getString("id"));
            MessageType messageType = MessageType.valueOf(rs.getString("type"));
            String subject = rs.getString("subject_line");
            String body = rs.getString("body");
            LocalDateTime sendAt = rs.getTimestamp("sent_at").toLocalDateTime();
            MessageState messageState = MessageState.valueOf(rs.getString("message_state"));
            String recipient = rs.getString("recipient");
            return new Message(id, messageType, messageState, subject, body, sendAt, recipient, attachments.getOrDefault(id, Collections.emptyList()));
        }
    }
}
