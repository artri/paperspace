package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class DocumentServiceTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private BinaryService binaryService;

    @Test
    void shouldStoreEmptyDocument() {
        Binary binaryId = storeBinary();
        Document document = new Document(UUID.randomUUID(), LocalDateTime.now(), "Test Title ä", "Test Description", binaryId, Collections.emptyList());
        this.documentService.store(document);
        Document storedDocument = this.documentService.getDocument(document.getId());
        assertEquals(document.getId(), storedDocument.getId());
        assertEquals(document.getCreatedAt(), storedDocument.getCreatedAt());
        assertEquals(document.getTitle(), storedDocument.getTitle());
        assertEquals(document.getDescription(), storedDocument.getDescription());
        assertEquals(document.getContent(), storedDocument.getContent());
        assertEquals(document.getFile(), storedDocument.getFile());

    }

    @Test
    void shouldStoreIncludingPagesDocument() {
        Binary binaryId = storeBinary();

        List<Page> pages = Arrays.asList(
                new Page(UUID.randomUUID(), 1, "Test Content 1", storeBinary()),
                new Page(UUID.randomUUID(), 3, "Test Content 3", storeBinary()),
                new Page(UUID.randomUUID(), 4, "Test Content 4", storeBinary())
        );
        Document document = new Document(UUID.randomUUID(), LocalDateTime.now(), "Test Title ä", "Test Description", binaryId, pages);
        this.documentService.store(document);
        Document storedDocument = this.documentService.getDocument(document.getId());
        assertEquals(document.getId(), storedDocument.getId());
        assertEquals(document.getCreatedAt(), storedDocument.getCreatedAt());
        assertEquals(document.getTitle(), storedDocument.getTitle());
        assertEquals(document.getDescription(), storedDocument.getDescription());
        assertEquals(document.getContent(), storedDocument.getContent());
        assertEquals(document.getFile(), storedDocument.getFile());
        assertEquals(3, storedDocument.getPages().size());

        assertEquals(pages.get(0).getId(), storedDocument.getPages().get(0).getId());
        assertEquals(pages.get(0).getContent(), storedDocument.getPages().get(0).getContent());
        assertEquals(pages.get(0).getNumber(), storedDocument.getPages().get(0).getNumber());

        assertEquals(pages.get(1).getId(), storedDocument.getPages().get(1).getId());
        assertEquals(pages.get(1).getContent(), storedDocument.getPages().get(1).getContent());
        assertEquals(pages.get(1).getNumber(), storedDocument.getPages().get(1).getNumber());

        assertEquals(pages.get(2).getId(), storedDocument.getPages().get(2).getId());
        assertEquals(pages.get(2).getContent(), storedDocument.getPages().get(2).getContent());
        assertEquals(pages.get(2).getNumber(), storedDocument.getPages().get(2).getNumber());
    }

    @Test
    void shouldUpdateTaskDocument() {
        TaskDocument taskDocument = new TaskDocument(UUID.randomUUID(), LocalDateTime.now(), "Test Task Title", null, storeBinary(), State.OPEN, Collections.emptyList(), null, null);
        TaskDocument storedDocument = this.documentService.store(taskDocument);

        LocalDateTime dueAt = LocalDateTime.now().plusDays(2);
        LocalDateTime doneAt = LocalDateTime.now().plusDays(3);
        this.documentService.update(
                storedDocument
                        .withDueAt(dueAt));

        storedDocument = (TaskDocument) this.documentService.getDocument(taskDocument.getId());
        assertEquals(dueAt, storedDocument.getDueAt());

        this.documentService.update(
                storedDocument
                        .withState(State.DONE)
                        .withDoneAt(doneAt));

        storedDocument = (TaskDocument) this.documentService.getDocument(taskDocument.getId());
        assertEquals(dueAt, storedDocument.getDueAt());
        assertEquals(State.DONE, storedDocument.getState());
        assertEquals(doneAt, storedDocument.getDoneAt());

        Page page = new Page(UUID.randomUUID(), 1, "TEST CONTENT", storeBinary());
        this.documentService.update(storedDocument.addPage(page));

        storedDocument = (TaskDocument) this.documentService.getDocument(taskDocument.getId());
        assertEquals("TEST CONTENT", storedDocument.getContent());
        assertEquals(1, storedDocument.getPages().size());
        assertEquals(page, storedDocument.getPages().get(0));
    }

    private Binary storeBinary() {
        Binary binary = new Binary(UUID.randomUUID(), LocalDateTime.now(), "Test Binary", "application/pdf", 100);
        this.binaryService.store(binary);
        return binary;
    }
}