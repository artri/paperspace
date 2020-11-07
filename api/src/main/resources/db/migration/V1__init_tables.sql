CREATE TABLE binaries
(
    id           VARCHAR(36)   NOT NULL PRIMARY KEY,
    created_at   DATETIME      NOT NULL,
    storage_path VARCHAR(2048) NOT NULL UNIQUE,
    hash         VARCHAR(32)   NOT NULL UNIQUE,
    mime_type    VARCHAR(255)  NOT NULL,
    length       BIGINT
);

CREATE TABLE documents
(
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    created_at  DATETIME     NOT NULL,
    due_at      DATETIME     NULL,
    done_at     DATETIME     NULL,
    title       VARCHAR(255),
    description TEXT,
    type        VARCHAR(255),
    state       VARCHAR(255) NULL,
    binary_id   VARCHAR(36)  NOT NULL,
    content     TEXT
);

CREATE TABLE pages
(
    id          VARCHAR(36) NOT NULL PRIMARY KEY,
    number      INT         NOT NULL,
    text        TEXT,
    document_id VARCHAR(36) NOT NULL,
    binary_id   VARCHAR(36)
);

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
);

CREATE TABLE processed_messages
(
    id            VARCHAR(36)  NOT NULL,
    type          VARCHAR(255) NOT NULL,
    recipient     TEXT         NOT NULL,
    subject_line  TEXT,
    body          TEXT         NOT NULL,
    sent_at       TIMESTAMP    NOT NULL,
    message_state VARCHAR(255) NOT NULL
);

CREATE TABLE processed_messages_attachments
(
    message_id VARCHAR(36)  NOT NULL,
    position   INT          NOT NULL,
    type       VARCHAR(255) NOT NULL,
    source_id  VARCHAR(36)  NOT NULL
);

CREATE TABLE outgoing_messages_attachments
(
    message_id VARCHAR(36)  NOT NULL,
    position   INT          NOT NULL,
    type       VARCHAR(255) NOT NULL,
    source_id  VARCHAR(36)  NOT NULL
);
