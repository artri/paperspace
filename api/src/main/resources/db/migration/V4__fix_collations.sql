ALTER TABLE outgoing_messages COLLATE = 'utf8mb4_unicode_ci';
ALTER TABLE outgoing_messages_attachments COLLATE = 'utf8mb4_unicode_ci';
ALTER TABLE processed_messages COLLATE = 'utf8mb4_unicode_ci';
ALTER TABLE processed_messages_attachments COLLATE = 'utf8mb4_unicode_ci';

ALTER TABLE processed_messages_attachments MODIFY message_id VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE processed_messages MODIFY id VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE outgoing_messages_attachments MODIFY message_id VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE outgoing_messages MODIFY id VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;