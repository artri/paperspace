CREATE TABLE binaries
(
    id                VARCHAR(36)  NOT NULL PRIMARY KEY,
    created_at        DATETIME     NOT NULL,
    original_filename VARCHAR(1024),
    mime_type         VARCHAR(255) NOT NULL,
    length            BIGINT
) CHARACTER SET utf8
  COLLATE utf8_unicode_ci;;

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
) CHARACTER SET utf8
  COLLATE utf8_unicode_ci;;

CREATE TABLE pages
(
    id          VARCHAR(36) NOT NULL PRIMARY KEY,
    number      INT         NOT NULL,
    text        TEXT CHARACTER SET utf8mb4,
    document_id VARCHAR(36) NOT NULL,
    binary_id   VARCHAR(36)
);
