package com.dedicatedcode.paperspace.mail;

import com.dedicatedcode.paperspace.TestHelper;
import com.dedicatedcode.paperspace.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class DocumentUploadMailSchedulerTest {

    @Autowired
    private DocumentUploadMailScheduler scheduler;

    @Autowired
    private JdbcMessageService messageService;

    @Test
    void shouldScheduleMailForDocument() {
        TestHelper.TestFile testFile = TestHelper.randPdf();
        Binary binary = new Binary(UUID.randomUUID(),LocalDateTime.now(), testFile.getFile().getAbsolutePath(), testFile.getHash(), "application/pdf", 123, OCRState.PROCESSED);
        Document document = new Document(UUID.randomUUID(), LocalDateTime.now(), "Test Document", "Description", binary, Collections.emptyList(), Collections.emptyList());

        scheduler.created(document);

        List<Message> messages = this.messageService.getScheduledMessageBy("DOCUMENT_CREATED_" + document.getId());
        assertEquals(1, messages.size());
    }

    @Test
    void shouldNotScheduleMailForTask() {
        TestHelper.TestFile testFile = TestHelper.randPdf();
        Binary binary = new Binary(UUID.randomUUID(),LocalDateTime.now(), testFile.getFile().getAbsolutePath(), testFile.getHash(), "application/pdf", 123, OCRState.PROCESSED);
        TaskDocument document = new TaskDocument(UUID.randomUUID(), LocalDateTime.now(), "Test Document", "Description", binary, State.OPEN, Collections.emptyList(), LocalDateTime.now(), null, Collections.emptyList());

        scheduler.created(document);

        List<Message> messages = this.messageService.getScheduledMessageBy("DOCUMENT_CREATED_" + document.getId());
        assertEquals(0, messages.size());

    }
}