package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
public class TagService {
    private final JdbcTemplate jdbcTemplate;

    public TagService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Tag get(UUID id) {
        return this.jdbcTemplate.query("SELECT * FROM tags WHERE id = ?",
                TagService::mapRow, id.toString())
                .stream()
                .findFirst()
                .orElse(null);
    }

    public List<Tag> getAll() {
        return getAll(null);
    }

    public List<Tag> getAll(String searchTerm) {
        if (searchTerm == null) {
            return this.jdbcTemplate.query("SELECT * FROM tags ORDER BY name", TagService::mapRow);
        } else {
            searchTerm = "%" + searchTerm + "%";
            return this.jdbcTemplate.query("SELECT * FROM tags WHERE name LIKE ? ORDER BY name", TagService::mapRow, searchTerm);
        }
    }

    public void store(Tag tag) {
        this.jdbcTemplate.update("INSERT INTO tags(id,name) VALUES (?,?)", tag.getId().toString(), tag.getName());
    }

    public void delete(Tag tag) {
        this.jdbcTemplate.update("DELETE FROM documents_tags WHERE tag_id = ?", tag.getId().toString());
        this.jdbcTemplate.update("DELETE FROM tags WHERE id = ?", tag.getId().toString());
    }

    public List<Tag> getUnassignedTags() {
        return this.jdbcTemplate.query("SELECT * FROM tags WHERE id NOT IN (SELECT tag_id FROM documents_tags)", TagService::mapRow);
    }

    private static Tag mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Tag(UUID.fromString(rs.getString("id")), rs.getString("name"));
    }

}
