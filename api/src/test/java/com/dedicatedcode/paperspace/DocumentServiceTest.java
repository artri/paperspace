package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.dedicatedcode.paperspace.AssertionUtils.DateTimeComparePrecision.SECONDS;
import static com.dedicatedcode.paperspace.AssertionUtils.assertDateTimeEquals;
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
        Document document = new Document(UUID.randomUUID(), LocalDateTime.now(), "Test Title ä", "Test Description", binaryId, Collections.emptyList(), Collections.emptyList());
        this.documentService.store(document);
        Document storedDocument = this.documentService.getDocument(document.getId());
        assertEquals(document.getId(), storedDocument.getId());
        assertDateTimeEquals(document.getCreatedAt(), storedDocument.getCreatedAt(), SECONDS);
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
        Document document = new Document(UUID.randomUUID(), LocalDateTime.now(), "Test Title ä", "Test Description", binaryId, pages, Collections.emptyList());
        this.documentService.store(document);
        Document storedDocument = this.documentService.getDocument(document.getId());
        assertEquals(document.getId(), storedDocument.getId());
        assertDateTimeEquals(document.getCreatedAt(), storedDocument.getCreatedAt(), SECONDS);
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
        TaskDocument taskDocument = new TaskDocument(UUID.randomUUID(), LocalDateTime.now(), "Test Task Title", null, storeBinary(), State.OPEN, Collections.emptyList(), null, null, Collections.emptyList());
        TaskDocument storedDocument = this.documentService.store(taskDocument);

        LocalDateTime dueAt = LocalDateTime.now().plusDays(2);
        LocalDateTime doneAt = LocalDateTime.now().plusDays(3);
        this.documentService.update(
                storedDocument
                        .withDueAt(dueAt));

        storedDocument = (TaskDocument) this.documentService.getDocument(taskDocument.getId());
        assertDateTimeEquals(dueAt, storedDocument.getDueAt(), SECONDS);

        this.documentService.update(
                storedDocument
                        .withState(State.DONE)
                        .withDoneAt(doneAt));

        storedDocument = (TaskDocument) this.documentService.getDocument(taskDocument.getId());
        assertDateTimeEquals(dueAt, storedDocument.getDueAt(), SECONDS);
        assertEquals(State.DONE, storedDocument.getState());
        assertDateTimeEquals(doneAt, storedDocument.getDoneAt(), SECONDS);

        Page page = new Page(UUID.randomUUID(), 1, "TEST CONTENT", storeBinary());
        this.documentService.update(storedDocument.withPages(Collections.singletonList(page)));

        storedDocument = (TaskDocument) this.documentService.getDocument(taskDocument.getId());
        assertEquals("TEST CONTENT", storedDocument.getContent());
        assertEquals(1, storedDocument.getPages().size());
        assertEquals(page, storedDocument.getPages().get(0));
    }

    private Binary storeBinary() {
        Binary binary = new Binary(UUID.randomUUID(),
                LocalDateTime.now(),
                System.getProperty("java.io.tmpdir") + File.separatorChar + UUID.randomUUID() + ".pdf",
                UUID.randomUUID().toString(),
                "application/pdf",
                100);
        this.binaryService.store(binary);
        return binary;
    }
}