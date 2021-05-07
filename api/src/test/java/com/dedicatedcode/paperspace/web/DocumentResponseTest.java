package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.Document;
import com.dedicatedcode.paperspace.model.Page;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.dedicatedcode.paperspace.TestHelper.randBinary;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentResponseTest {

    @Test
    void ensureLinksAreSet() {
        UUID taskId = UUID.randomUUID();

        List<Page> pages = Arrays.asList(
                new Page(UUID.randomUUID(), 1, "Test Content 1", randBinary()),
                new Page(UUID.randomUUID(), 3, "Test Content 3", randBinary()),
                new Page(UUID.randomUUID(), 4, "Test Content 4", randBinary())
        );
        Binary binary = randBinary();
        Document taskDocument = new Document(taskId, LocalDateTime.now(), "Test Task", "", binary, pages, Collections.emptyList());

        DocumentResponse response = new DocumentResponse(taskDocument);
        assertEquals("/document/" + taskId, response.getLinks().get("self"));
        assertEquals("/api/document/" + taskId, response.getLinks().get("edit"));
        assertEquals("/document/edit/" + taskId, response.getLinks().get("editPages"));
        assertEquals("/api/document/" + taskId + "/pages", response.getLinks().get("pages"));
        assertEquals("/api/download/" + binary.getId(), response.getLinks().get("download"));
        assertEquals("/api/view/" + binary.getId(), response.getLinks().get("view"));
        assertEquals("/api/image/" + pages.get(0).getPreview().getId() + "?width=560", response.getLinks().get("preview"));
    }
}