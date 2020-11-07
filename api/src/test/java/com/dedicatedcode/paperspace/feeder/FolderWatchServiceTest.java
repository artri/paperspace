package com.dedicatedcode.paperspace.feeder;

import com.dedicatedcode.paperspace.BinaryService;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FolderWatchServiceTest {
    private static final Logger log = LoggerFactory.getLogger(FolderWatchServiceTest.class);
    private FolderWatchService instance;
    private File documentPath;
    private MergingFileEventHandler fileEventHandler;

    @BeforeEach
    void setUp() {
        ExecutorService threadPool = Executors.newWorkStealingPool();
        this.documentPath = Files.newTemporaryFolder();
        this.fileEventHandler = mock(MergingFileEventHandler.class);
        this.instance = new FolderWatchService(threadPool, documentPath, null, 1, mock(BinaryService.class), fileEventHandler);
    }

    @AfterEach
    void tearDown() {
        this.instance.stop();
    }

    @Test
    @Timeout(5)
    void shouldFireFileEventsOnRootFolder() throws Exception {
        File targetDocumentFile = new File(documentPath, UUID.randomUUID().toString());
        FileUtils.copyToFile(getClass().getResourceAsStream("/test_files/A Sample PDF.pdf"), targetDocumentFile);
        Thread.sleep(1000);

        verify(fileEventHandler).register(EventType.CREATE, targetDocumentFile, InputType.DOCUMENT);
    }

    @Test
    @Timeout(5)
    void shouldFireFileEventsOnSubFolder() throws IOException, InterruptedException {
        File subDirectory = new File(documentPath, "/test/folder_" + UUID.randomUUID().toString());
        assertTrue(subDirectory.mkdirs());
        File targetDocumentFile = new File(subDirectory, "pdf_" + UUID.randomUUID().toString());
        FileUtils.touch(targetDocumentFile);
        FileUtils.copyToFile(getClass().getResourceAsStream("/test_files/A Sample PDF.pdf"), targetDocumentFile);

        Thread.sleep(1000);
        verify(fileEventHandler).register(EventType.CREATE, targetDocumentFile, InputType.DOCUMENT);
    }


    @Test
    @Timeout(5)
    void shouldFireFileEventsOnFolderMoveIntoPath() throws IOException {
        File sourceTempFolder = Files.newTemporaryFolder();
        File subDirectory = new File(sourceTempFolder, "/test/folder/");
        assertTrue(subDirectory.mkdirs());

        File targetDocumentFile = new File(subDirectory, "pdf_" + UUID.randomUUID().toString());
        FileUtils.copyToFile(getClass().getResourceAsStream("/test_files/A Sample PDF.pdf"), targetDocumentFile);

        File targetFolder = this.documentPath;
        FileUtils.moveDirectoryToDirectory(sourceTempFolder, targetFolder, true);

        String expectedPathName = targetFolder.getAbsolutePath() + "/" + sourceTempFolder.getName() + "/test/folder/" + targetDocumentFile.getName();

        verify(fileEventHandler).register(EventType.CREATE, new File(expectedPathName), InputType.DOCUMENT);
    }

    @Test
    void shouldFireForPreExistingFiles() throws IOException {
        File rootFolder = Files.newTemporaryFolder();
        File subDirectory = new File(rootFolder, "/sub/");

        File fileInSubFolder = new File(subDirectory, "pdf_" + UUID.randomUUID().toString());
        File fileInRootFolder = new File(rootFolder, "pdf_" + UUID.randomUUID().toString());
        FileUtils.copyToFile(getClass().getResourceAsStream("/test_files/A Sample PDF.pdf"), fileInRootFolder);
        FileUtils.copyToFile(getClass().getResourceAsStream("/test_files/A Sample PDF.pdf"), fileInSubFolder);

        ExecutorService threadPool = Executors.newWorkStealingPool();

        MergingFileEventHandler fileEventHandler = mock(MergingFileEventHandler.class);
        new FolderWatchService(threadPool, rootFolder, null, 1, mock(BinaryService.class), fileEventHandler);

        verify(fileEventHandler).register(EventType.EXISTING, fileInSubFolder, InputType.DOCUMENT);
        verify(fileEventHandler).register(EventType.EXISTING, fileInRootFolder, InputType.DOCUMENT);
    }

    private void await(int expectedNumberOfEvents, RecordingEventListener listener) throws InterruptedException {
        while (listener.getFiles().size() < expectedNumberOfEvents) {
            log.info("waiting for file operations to finish because nr of events [{}] is smaller than expected [{}]", listener.getFiles().size(), expectedNumberOfEvents);
            Thread.sleep(250);
        }
    }

    private static final class RecordingEventListener implements FileEventListener {

        private final List<File> files = new ArrayList<>();

        @Override
        public void handle(EventType eventType, File file, InputType type) {
            this.files.add(file);
        }

        public List<File> getFiles() {
            return files;
        }

    }
}