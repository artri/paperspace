CREATE TABLE tags
(
    id   VARCHAR(36)  NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE documents_tags
(
    document_id VARCHAR(36) NOT NULL,
    tag_id      VARCHAR(36) NOT NULL,
    FOREIGN KEY (document_id) REFERENCES documents (id),
    FOREIGN KEY (tag_id) REFERENCES tags (id)
);
