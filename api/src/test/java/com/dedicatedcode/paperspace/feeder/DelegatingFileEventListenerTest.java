package com.dedicatedcode.paperspace.feeder;

import com.dedicatedcode.paperspace.BinaryService;
import com.dedicatedcode.paperspace.DocumentService;
import com.dedicatedcode.paperspace.StorageService;
import com.dedicatedcode.paperspace.model.*;
import com.dedicatedcode.paperspace.search.SolrService;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


class DelegatingFileEventListenerTest {

    private final File documentLocation = Files.newTemporaryFolder();

    private DelegatingFileEventListener listener;
    private PdfOcrService ocrService;
    private SolrService solrService;
    private StorageService storageService;
    private DocumentService documentService;
    private BinaryService binaryService;

    @BeforeEach
    void setUp() {
        this.ocrService = mock(PdfOcrService.class);
        this.solrService = mock(SolrService.class);
        this.storageService = mock(StorageService.class);
        this.documentService = mock(DocumentService.class);
        this.binaryService = mock(BinaryService.class);
        List<DocumentListener> documentListeners = Collections.emptyList();
        this.listener = new DelegatingFileEventListener(binaryService, documentService, storageService, solrService, Collections.singletonList(ocrService), 14, documentListeners);
    }

    @Test
    void shouldHandleNewDocument() throws IOException, OcrException {
        String fileName = "test_pdf_" + UUID.randomUUID() + ".pdf";
        File targetFile = copyFile(fileName);

        when(this.binaryService.getByPath(targetFile.getAbsolutePath())).thenReturn(null);
        when(this.binaryService.getByHash(anyString())).thenReturn(null);
        Binary preview = new Binary(UUID.randomUUID(), LocalDateTime.now(), "/storage/test.png", "12345", "image/png", 1, OCRState.UNNECESSARY);
        Page page = new Page(UUID.randomUUID(), 1, "Test Content", preview);
        when(this.ocrService.doOcr(targetFile)).thenReturn(Collections.singletonList(page));

        Document documentMock = mock(Document.class);
        when(this.documentService.store(any(Document.class))).thenReturn(documentMock);
        listener.handle(EventType.CREATE, targetFile, InputType.DOCUMENT);


        verify(this.documentService, times(1)).store(any(Document.class));
        verify(this.solrService, times(1)).index(documentMock);
    }

    private File copyFile(String fileName) throws IOException {
        File targetFile = new File(this.documentLocation, fileName);
        FileUtils.copyToFile(getClass().getResourceAsStream("/test_files/A Sample PDF.pdf"), targetFile);
        return targetFile;
    }

    @Test
    void shouldHandleNewTask() throws IOException, OcrException {
        String fileName = "test_pdf_" + UUID.randomUUID() + ".pdf";
        File targetFile = copyFile(fileName);

        when(this.binaryService.getByPath(targetFile.getAbsolutePath())).thenReturn(null);
        when(this.binaryService.getByHash(anyString())).thenReturn(null);
        Binary preview = new Binary(UUID.randomUUID(), LocalDateTime.now(), "/storage/test.png", "12345", "image/png", 1, OCRState.UNNECESSARY);
        Page page = new Page(UUID.randomUUID(), 1, "Test Content", preview);
        when(this.ocrService.doOcr(targetFile)).thenReturn(Collections.singletonList(page));

        TaskDocument taskMock = mock(TaskDocument.class);
        when(this.documentService.store(any(TaskDocument.class))).thenReturn(taskMock);
        listener.handle(EventType.CREATE, targetFile, InputType.TASK);

        verify(this.documentService, times(1)).store(any(TaskDocument.class));
        verify(this.solrService, times(1)).index(taskMock);
    }

    @Test
    void shouldHandleDeletionOfFile() {
        File deletedFile = new File(UUID.randomUUID() + ".pdf");

        Binary storedBinary = new Binary(UUID.randomUUID(), LocalDateTime.now(), deletedFile.getAbsolutePath(), "12345", "application/pdf", 1, OCRState.OPEN);
        Binary storedPgeBinary = new Binary(UUID.randomUUID(), LocalDateTime.now(), deletedFile.getAbsolutePath(), "12345", "application/pdf", 1, OCRState.OPEN);
        Document document = new Document(UUID.randomUUID(), LocalDateTime.now(), "Test Document", null, storedBinary,
                Collections.singletonList(new Page(UUID.randomUUID(), 1, "Test Content", storedPgeBinary)), Collections.emptyList());

        when(this.binaryService.getByPath(deletedFile.getAbsolutePath())).thenReturn(storedBinary);
        when(this.documentService.getByBinary(storedBinary.getId())).thenReturn(document);

        listener.handle(EventType.DELETE, deletedFile, InputType.TASK);

        verify(this.binaryService, times(1)).delete(storedBinary);
        verify(this.binaryService, times(1)).delete(storedPgeBinary);
        verify(this.documentService, times(1)).delete(document);
        verify(this.solrService, times(1)).delete(document);
    }

    @Test
    void shouldUpdateBinary() throws IOException {
        File targetFile = copyFile(UUID.randomUUID() + ".pdf");

        Binary storedBinary = new Binary(UUID.randomUUID(), LocalDateTime.now(), "/some/old/path", "12345", "application/pdf", 1, OCRState.OPEN);

        when(binaryService.getByPath(targetFile.getAbsolutePath())).thenReturn(null);
        when(binaryService.getByHash("e572fdbb0a1ce8d3b77c5f4e59db82fe")).thenReturn(storedBinary);
        Document associatedDocument = mock(Document.class);
        when(documentService.getByBinary(storedBinary.getId())).thenReturn(associatedDocument);
        listener.handle(EventType.CHANGE, targetFile, InputType.DOCUMENT);

        verify(this.binaryService, times(1)).update(storedBinary);
    }

    @Test
    void shouldSwitchToTaskFromDocument() throws IOException {
        File targetFile = copyFile(UUID.randomUUID() + ".pdf");

        Binary storedBinary = new Binary(UUID.randomUUID(), LocalDateTime.now(), "/some/old/path", "12345", "application/pdf", 1, OCRState.OPEN);
        Binary storedPageBinary = new Binary(UUID.randomUUID(), LocalDateTime.now(), targetFile.getAbsolutePath(), "12345", "application/pdf", 1, OCRState.OPEN);
        Document document = new Document(UUID.randomUUID(), LocalDateTime.now(), "Test Document", null, storedBinary,
                Collections.singletonList(new Page(UUID.randomUUID(), 1, "Test Content", storedPageBinary)), Collections.emptyList());

        when(binaryService.getByPath(targetFile.getAbsolutePath())).thenReturn(null);
        when(binaryService.getByHash("e572fdbb0a1ce8d3b77c5f4e59db82fe")).thenReturn(storedBinary);

        when(documentService.getByBinary(storedBinary.getId())).thenReturn(document);

        listener.handle(EventType.MOVE, targetFile, InputType.TASK);

        verify(this.binaryService, times(1)).update(storedBinary);

        verify(this.documentService, times(1)).delete(document);
        verify(this.solrService, times(1)).delete(document);
        verify(this.documentService, times(1)).store(any(TaskDocument.class));
        verify(this.solrService, times(1)).index(any(Document.class));
    }
}