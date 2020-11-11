package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Service
public class BinaryService {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public BinaryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void store(Binary binary) {
        this.jdbcTemplate.update("INSERT INTO binaries(id, created_at, storage_path, hash, mime_type, length) VALUES(?,?,?,?,?,?)",
                binary.getId().toString(),
                Timestamp.valueOf(binary.getCreatedAt()),
                binary.getStorageLocation(),
                binary.getHash(),
                binary.getMimeType(),
                binary.getLength());
    }


    public void update(Binary binary) {
        this.jdbcTemplate.update("UPDATE binaries SET created_at = ?, storage_path = ?, hash = ?, mime_type = ?, length = ? WHERE id = ?",
                Timestamp.valueOf(binary.getCreatedAt()),
                binary.getStorageLocation(),
                binary.getHash(),
                binary.getMimeType(),
                binary.getLength(),
                binary.getId().toString());

    }

    public Binary get(UUID id) {
        return this.jdbcTemplate.query("SELECT * FROM binaries WHERE id = ?", (rs, rowNum) ->
                new Binary(
                        UUID.fromString(rs.getString("id")),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getString("storage_path"),
                        rs.getString("hash"),
                        rs.getString("mime_type"),
                        rs.getLong("length")), id.toString())
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Binary getByPath(String storagePath) {
        return this.jdbcTemplate.query("SELECT * FROM binaries WHERE storage_path = ?", (rs, rowNum) ->
                new Binary(
                        UUID.fromString(rs.getString("id")),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getString("storage_path"),
                        rs.getString("hash"),
                        rs.getString("mime_type"),
                        rs.getLong("length")), storagePath)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Binary getByHash(String md5) {
        return this.jdbcTemplate.query("SELECT * FROM binaries WHERE hash = ?", (rs, rowNum) ->
                new Binary(
                        UUID.fromString(rs.getString("id")),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getString("storage_path"),
                        rs.getString("hash"),
                        rs.getString("mime_type"),
                        rs.getLong("length")), md5)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public void delete(Binary binary) {
        this.jdbcTemplate.update("DELETE FROM binaries WHERE id = ?", binary.getId().toString());

    }

    public List<Binary> getAll() {
        return this.jdbcTemplate.query("SELECT * FROM binaries ", (rs, rowNum) ->
                new Binary(
                        UUID.fromString(rs.getString("id")),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getString("storage_path"),
                        rs.getString("hash"),
                        rs.getString("mime_type"),
                        rs.getLong("length")));
    }
}
