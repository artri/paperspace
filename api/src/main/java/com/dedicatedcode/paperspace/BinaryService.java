package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.OCRState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
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
        this.jdbcTemplate.update("INSERT INTO binaries(id, created_at, storage_path, hash, mime_type, length, state) VALUES(?,?,?,?,?,?,?)",
                binary.getId().toString(),
                Timestamp.valueOf(binary.getCreatedAt()),
                binary.getStorageLocation(),
                binary.getHash(),
                binary.getMimeType(),
                binary.getLength(),
                binary.getState().name());
    }


    public void update(Binary binary) {
        this.jdbcTemplate.update("UPDATE binaries SET created_at = ?, storage_path = ?, hash = ?, mime_type = ?, length = ?, state = ? WHERE id = ?",
                Timestamp.valueOf(binary.getCreatedAt()),
                binary.getStorageLocation(),
                binary.getHash(),
                binary.getMimeType(),
                binary.getLength(),
                binary.getState().name(),
                binary.getId().toString());

    }

    public Binary get(UUID id) {
        return this.jdbcTemplate.query("SELECT * FROM binaries WHERE id = ?", getBinaryRowMapper(), id.toString())
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Binary getByPath(String storagePath) {
        return this.jdbcTemplate.query("SELECT * FROM binaries WHERE storage_path = ?", getBinaryRowMapper(), storagePath)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Binary getByHash(String md5) {
        return this.jdbcTemplate.query("SELECT * FROM binaries WHERE hash = ?", getBinaryRowMapper(), md5)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public void delete(Binary binary) {
        this.jdbcTemplate.update("DELETE FROM binaries WHERE id = ?", binary.getId().toString());

    }

    public List<Binary> getAll() {
        return this.jdbcTemplate.query("SELECT * FROM binaries ", getBinaryRowMapper());
    }

    private RowMapper<Binary> getBinaryRowMapper() {
        return (rs, rowNum) ->
                new Binary(
                        UUID.fromString(rs.getString("id")),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getString("storage_path"),
                        rs.getString("hash"),
                        rs.getString("mime_type"),
                        rs.getLong("length"),
                        OCRState.valueOf(rs.getString("state")));
    }

    public List<Binary> getFailed() {
        return this.jdbcTemplate.query("SELECT * FROM binaries WHERE state = 'FAILED'", getBinaryRowMapper());
    }
}
