package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.UUID;

@Service
public class BinaryService {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public BinaryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void store(Binary binary) {
        this.jdbcTemplate.update("INSERT INTO binaries(id,created_at, original_filename, mime_type, length) VALUES(?,?,?,?,?)",
                binary.getId().toString(),
                Timestamp.valueOf(binary.getCreatedAt()),
                binary.getOriginalFileName(),
                binary.getMimeType(),
                binary.getLength());
    }

    public Binary get(UUID id) {
        return this.jdbcTemplate.query("SELECT * FROM binaries WHERE id = ?", (rs, rowNum) ->
                new Binary(
                        UUID.fromString(rs.getString("id")),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getString("original_filename"),
                        rs.getString("mime_type"),
                        rs.getLong("length")), id.toString())
                .stream()
                .findFirst()
                .orElse(null);
    }
}
