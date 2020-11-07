package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.dedicatedcode.paperspace.SQLHelper.nullableDateTime;
import static com.dedicatedcode.paperspace.SQLHelper.nullableTimestamp;

@Service
public class DocumentService {
    private final JdbcTemplate jdbcTemplate;
    private final BinaryService binaryService;

    @Autowired
    public DocumentService(JdbcTemplate jdbcTemplate, BinaryService binaryService) {
        this.jdbcTemplate = jdbcTemplate;
        this.binaryService = binaryService;
    }

    public Document store(Document document) {
        this.jdbcTemplate.update("INSERT INTO documents(id, created_at, title, description, binary_id, content,type, state, due_at,done_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                document.getId().toString(),
                Timestamp.valueOf(document.getCreatedAt()),
                document.getTitle(),
                document.getDescription(),
                document.getFile().getId().toString(),
                document.getContent(),
                "DOCUMENT",
                null,
                null,
                null);
        storePages(document);
        return document;
    }

    public TaskDocument store(TaskDocument document) {
        this.jdbcTemplate.update("INSERT INTO documents(id, created_at, title, description, binary_id, content,type, state, due_at,done_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                document.getId().toString(),
                Timestamp.valueOf(document.getCreatedAt()),
                document.getTitle(),
                document.getDescription(),
                document.getFile().getId().toString(),
                document.getContent(),
                "TASK",
                document.getState().toString(),
                nullableTimestamp(document.getDueAt()),
                nullableTimestamp(document.getDoneAt()));
        storePages(document);
        return document;
    }

    public void update(Document document) {
        this.jdbcTemplate.update("UPDATE documents SET title = ?, description = ?, content = ? WHERE id = ?",
                document.getTitle(),
                document.getDescription(),
                document.getContent(),
                document.getId().toString());
        this.jdbcTemplate.update("DELETE FROM pages WHERE document_id = ?", document.getId().toString());
        storeTags(document);
        storePages(document);
    }

    public void update(TaskDocument document) {
        this.jdbcTemplate.update(
                "UPDATE documents SET title = ?, description = ?, content = ?, state = ?, due_at = ?, done_at = ? WHERE id = ?",
                document.getTitle(),
                document.getDescription(),
                document.getContent(),
                document.getState().name(),
                nullableTimestamp(document.getDueAt()),
                nullableTimestamp(document.getDoneAt()),
                document.getId().toString());
        this.jdbcTemplate.update("DELETE FROM pages WHERE document_id = ?", document.getId().toString());
        storeTags(document);
        storePages(document);
    }

    private void storeTags(Document document) {
        this.jdbcTemplate.update("DELETE FROM documents_tags WHERE document_id = ?", document.getId().toString());
        document.getTags().forEach(tag -> this.jdbcTemplate.update("INSERT INTO documents_tags(document_id, tag_id) VALUES (?,?)", document.getId().toString(), tag.getId().toString()));
    }

    public Document getDocument(UUID id) {
        return this.jdbcTemplate.query("SELECT * FROM documents WHERE id = ?", this::mapDocumentRow
                , id.toString())
                .stream()
                .findFirst()
                .map(this::toDocument).orElse(null);
    }

    public Document getByBinary(UUID binaryId) {
        return this.jdbcTemplate.query("SELECT * FROM documents WHERE binary_id = ?", this::mapDocumentRow
                , binaryId.toString())
                .stream()
                .findFirst()
                .map(this::toDocument).orElse(null);
    }


    private void storePages(Document document) {
        document.getPages().forEach(page -> this.jdbcTemplate.update("INSERT INTO pages(id,number,text,document_id,binary_id) VALUES (?,?,?,?,?)",
                page.getId().toString(),
                page.getNumber(),
                page.getContent(),
                document.getId().toString(),
                page.getPreview().getId().toString())
        );
    }

    public List<Document> getAll() {
        return this.jdbcTemplate.query("SELECT * FROM documents", this::mapDocumentRow)
                .stream()
                .map(this::toDocument)
                .collect(Collectors.toList());
    }

    private Document toDocument(DocumentDataHolder documentDataHolder) {
        Binary mainBinary = binaryService.get(documentDataHolder.binaryId);
        List<Page> pages = this.jdbcTemplate.query("SELECT * FROM pages WHERE document_id = ? ORDER BY number",
                (rs, rowNum) -> new Page(
                        UUID.fromString(rs.getString("id")),
                        rs.getInt("number"),
                        rs.getString("text"),
                        binaryService.get(UUID.fromString(rs.getString("binary_id")))), documentDataHolder.id.toString());
        List<Tag> tags = this.jdbcTemplate.query("SELECT * FROM tags WHERE id IN (SELECT tag_id FROM documents_tags WHERE document_id = ?)", (rs, rowNum) -> new Tag(UUID.fromString(rs.getString("id")), rs.getString("name")), documentDataHolder.id.toString());
        switch (documentDataHolder.documentType) {
            case "DOCUMENT":
                return new Document(
                        documentDataHolder.id,
                        documentDataHolder.createdAt,
                        documentDataHolder.title,
                        documentDataHolder.description,
                        mainBinary,
                        documentDataHolder.content,
                        pages,
                        tags);
            case "TASK":
                return new TaskDocument(
                        documentDataHolder.id,
                        documentDataHolder.createdAt,
                        documentDataHolder.title,
                        documentDataHolder.description,
                        mainBinary,
                        State.valueOf(documentDataHolder.state),
                        documentDataHolder.content,
                        pages,
                        documentDataHolder.dueAt,
                        documentDataHolder.doneAt,
                        tags);
            default:
                throw new IllegalStateException("Unexpected value: " + documentDataHolder.documentType);
        }
    }
    private DocumentDataHolder mapDocumentRow(ResultSet rs, int rowNum) throws SQLException {
        return new DocumentDataHolder(
                UUID.fromString(rs.getString("id")),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getString("title"),
                rs.getString("description"),
                UUID.fromString(rs.getString("binary_id")),
                rs.getString("content"),
                rs.getString("type"),
                rs.getString("state"),
                nullableDateTime(rs, "due_at"),
                nullableDateTime(rs, "done_at"));
    }

    public void delete(Document document) {
        document.getPages().forEach(page -> this.jdbcTemplate.update("DELETE FROM pages WHERE id = ?", page.getId().toString()));
        this.jdbcTemplate.update("DELETE FROM documents WHERE id = ?", document.getId().toString());
    }

    private static class DocumentDataHolder {
        private final UUID id;
        private final LocalDateTime createdAt;
        private final String title;
        private final String description;
        private final UUID binaryId;
        private final String content;
        private final String documentType;
        private final String state;
        private final LocalDateTime dueAt;
        private final LocalDateTime doneAt;

        public DocumentDataHolder(UUID id, LocalDateTime createdAt, String title, String description, UUID binaryId, String content, String documentType, String state, LocalDateTime dueAt, LocalDateTime doneAt) {
            this.id = id;
            this.createdAt = createdAt;
            this.title = title;
            this.description = description;
            this.binaryId = binaryId;
            this.content = content;
            this.documentType = documentType;
            this.state = state;
            this.dueAt = dueAt;
            this.doneAt = doneAt;
        }
    }
}
