package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.Page;
import com.dedicatedcode.paperspace.model.State;
import com.dedicatedcode.paperspace.model.TaskDocument;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.dedicatedcode.paperspace.TestHelper.randBinary;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskDocumentResponseTest {

    @Test
    void ensureLinksAreSet() {
        UUID taskId = UUID.randomUUID();

        List<Page> pages = Arrays.asList(
                new Page(UUID.randomUUID(), 1, "Test Content 1", randBinary()),
                new Page(UUID.randomUUID(), 3, "Test Content 3", randBinary()),
                new Page(UUID.randomUUID(), 4, "Test Content 4", randBinary())
        );
        Binary binary = randBinary();
        TaskDocument taskDocument = new TaskDocument(taskId, LocalDateTime.now(), "Test Task", "", binary, State.OPEN, pages, null, null, Collections.emptyList());

        TaskDocumentResponse response = new TaskDocumentResponse(taskDocument);
        assertEquals("/task/" + taskId, response.getLinks().get("self"));
        assertEquals("/api/task/" + taskId, response.getLinks().get("edit"));
        assertEquals("/task/edit/" + taskId, response.getLinks().get("editPages"));
        assertEquals("/api/task/" + taskId + "/pages", response.getLinks().get("pages"));
        assertEquals("/api/task/" + taskId + "/done", response.getLinks().get("done"));
        assertEquals("/api/download/" + binary.getId(), response.getLinks().get("download"));
        assertEquals("/api/view/" + binary.getId(), response.getLinks().get("view"));
        assertEquals("/api/image/" + pages.get(0).getPreview().getId() + "?width=560", response.getLinks().get("preview"));
    }
}