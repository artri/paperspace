CREATE TABLE outgoing_messages
(
    id            VARCHAR(36)  NOT NULL,
    type          VARCHAR(255) NOT NULL,
    identifier    VARCHAR(255) NOT NULL,
    subject_line  TEXT,
    recipient     TEXT         NOT NULL,
    body          TEXT         NOT NULL,
    send_at       TIMESTAMP    NOT NULL,
    message_state VARCHAR(255) NOT NULL
) CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE TABLE processed_messages
(
    id            VARCHAR(36)  NOT NULL,
    type          VARCHAR(255) NOT NULL,
    recipient     TEXT         NOT NULL,
    subject_line  TEXT,
    body          TEXT         NOT NULL,
    sent_at       TIMESTAMP    NOT NULL,
    message_state VARCHAR(255) NOT NULL
) CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE TABLE processed_messages_attachments
(
    message_id VARCHAR(36)  NOT NULL,
    position   INT          NOT NULL,
    type       VARCHAR(255) NOT NULL,
    source_id  VARCHAR(36)  NOT NULL,
    INDEX idx_processed_message_id (message_id)
);

CREATE TABLE outgoing_messages_attachments
(
    message_id VARCHAR(36)  NOT NULL,
    position   INT          NOT NULL,
    type       VARCHAR(255) NOT NULL,
    source_id  VARCHAR(36)  NOT NULL,
    INDEX idx_outgoing_message_id (message_id)
)