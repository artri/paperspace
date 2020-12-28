package com.dedicatedcode.paperspace.feeder;

import com.dedicatedcode.paperspace.BinaryService;
import com.dedicatedcode.paperspace.DocumentService;
import com.dedicatedcode.paperspace.StorageService;
import com.dedicatedcode.paperspace.model.*;
import com.dedicatedcode.paperspace.search.SolrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.dedicatedcode.paperspace.TestHelper.TestFile;
import static com.dedicatedcode.paperspace.TestHelper.randPdf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


class DelegatingFileEventListenerTest {

    private DelegatingFileEventListener listener;
    private PdfOcrService ocrService;
    private SolrService solrService;
    private DocumentService documentService;
    private BinaryService binaryService;

    @BeforeEach
    void setUp() {
        this.ocrService = mock(PdfOcrService.class);
        when(this.ocrService.supportedFileFormats()).thenReturn(Collections.singletonList("application/pdf"));
        this.solrService = mock(SolrService.class);
        this.documentService = mock(DocumentService.class);
        this.binaryService = mock(BinaryService.class);
        List<DocumentListener> documentListeners = Collections.emptyList();
        this.listener = new DelegatingFileEventListener(binaryService, documentService, mock(StorageService.class), solrService, Collections.singletonList(ocrService), 14, documentListeners);
    }

    @Test
    void shouldHandleNewDocument() throws OcrException {
        File targetFile = randPdf().getFile();

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

    @Test
    void shouldHandleNewTask() throws OcrException {
        File targetFile = randPdf().getFile();

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
    void shouldUpdateBinary() {
        TestFile testFile = randPdf();
        File targetFile = testFile.getFile();

        Binary storedBinary = new Binary(UUID.randomUUID(), LocalDateTime.now(), "/some/old/" + testFile.getFile().getName(), "12345", "application/pdf", 1, OCRState.OPEN);

        when(binaryService.getByPath(targetFile.getAbsolutePath())).thenReturn(null);
        when(binaryService.getByHash(testFile.getHash())).thenReturn(storedBinary);
        Document associatedDocument = mock(Document.class);
        when(documentService.getByBinary(storedBinary.getId())).thenReturn(associatedDocument);
        listener.handle(EventType.CHANGE, targetFile, InputType.DOCUMENT);

        verify(this.binaryService, times(1)).update(storedBinary);
    }

    @Test
    void shouldSwitchToTaskFromDocument() {
        TestFile testFile = randPdf();
        File targetFile = testFile.getFile();

        Binary storedBinary = new Binary(UUID.randomUUID(), LocalDateTime.now(), "/some/old/" + testFile.getFile().getName(), "12345", "application/pdf", 1, OCRState.OPEN);
        Binary storedPageBinary = new Binary(UUID.randomUUID(), LocalDateTime.now(), targetFile.getAbsolutePath(), "12345", "application/pdf", 1, OCRState.OPEN);
        Document document = new Document(UUID.randomUUID(), LocalDateTime.now(), "Test Document", null, storedBinary,
                Collections.singletonList(new Page(UUID.randomUUID(), 1, "Test Content", storedPageBinary)), Collections.emptyList());

        when(binaryService.getByPath(targetFile.getAbsolutePath())).thenReturn(null);
        when(binaryService.getByHash(testFile.getHash())).thenReturn(storedBinary);

        when(documentService.getByBinary(storedBinary.getId())).thenReturn(document);

        listener.handle(EventType.MOVE, targetFile, InputType.TASK);

        verify(this.binaryService, times(1)).update(storedBinary);

        verify(this.documentService, times(1)).delete(document);
        verify(this.solrService, times(1)).delete(document);
        verify(this.documentService, times(1)).store(any(TaskDocument.class));
        verify(this.solrService, times(1)).index(any(Document.class));
    }
}