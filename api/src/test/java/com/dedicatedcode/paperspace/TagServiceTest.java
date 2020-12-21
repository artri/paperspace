package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.dedicatedcode.paperspace.TestHelper.rand;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class TagServiceTest {

    @Autowired
    private TagService tagService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void storeNewTag() {
        Tag tag = new Tag(UUID.randomUUID(), rand());
        this.tagService.store(tag);

        assertNotNull(this.tagService.get(tag.getId()));
        assertEquals(tag.getName(), this.tagService.get(tag.getId()).getName());
        assertTrue(this.tagService.getAll().stream().anyMatch(t -> t.getId().equals(tag.getId())));
    }

    @Test
    void shouldRetrieveUnassignedTags() {
        Tag unassignedTag = new Tag(UUID.randomUUID(), rand());
        Tag assignedTag = new Tag(UUID.randomUUID(), rand());

        this.tagService.store(assignedTag);
        this.tagService.store(unassignedTag);
        this.jdbcTemplate.update("INSERT INTO documents_tags(document_id, tag_id) VALUES (?,?)", UUID.randomUUID().toString(), assignedTag.getId().toString());
        assertTrue(this.tagService.getUnassignedTags().stream().anyMatch(tag -> tag.equals(unassignedTag)));
        assertTrue(this.tagService.getUnassignedTags().stream().noneMatch(tag -> tag.equals(assignedTag)));
    }

    @Test
    void deleteTags() {
        Tag assignedTag = new Tag(UUID.randomUUID(), rand());

        this.tagService.store(assignedTag);
        this.jdbcTemplate.update("INSERT INTO documents_tags(document_id, tag_id) VALUES (?,?)", UUID.randomUUID().toString(), assignedTag.getId().toString());

        this.tagService.delete(assignedTag);

        assertEquals(0, this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tags WHERE id = ?", Long.class, assignedTag.getId().toString()));
        assertEquals(0, this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM documents_tags WHERE tag_id = ?", Long.class, assignedTag.getId().toString()));
    }

    @Test
    void searchTags() {
        long allTagsCount = this.tagService.getAll().size();

        Tag first = new Tag(UUID.randomUUID(), rand());
        Tag second = new Tag(UUID.randomUUID(), rand("searchable_"));
        Tag third = new Tag(UUID.randomUUID(), rand("searchable_"));

        this.tagService.store(first);
        this.tagService.store(second);
        this.tagService.store(third);

        List<Tag> result = this.tagService.getAll("search");
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(r -> second.getId().equals(r.getId())));
        assertTrue(result.stream().anyMatch(r -> third.getId().equals(r.getId())));

        result = this.tagService.getAll(null);
        assertEquals(allTagsCount + 3, result.size());
    }
}