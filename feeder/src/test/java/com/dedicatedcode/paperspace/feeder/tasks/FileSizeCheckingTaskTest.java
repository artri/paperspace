package com.dedicatedcode.paperspace.feeder.tasks;

import com.dedicatedcode.paperspace.feeder.FileStatus;
import com.dedicatedcode.paperspace.feeder.InputType;
import com.dedicatedcode.paperspace.feeder.configuration.ApiConfiguration;
import com.dedicatedcode.paperspace.feeder.configuration.AppConfiguration;
import com.dedicatedcode.paperspace.feeder.configuration.DocumentConfiguration;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSizeCheckingTaskTest {

    @Test
    void shouldDeleteInputIfNotSetToMoveToProcessedFolder() throws IOException {
        DocumentConfiguration documentConfiguration = new DocumentConfiguration();
        DocumentConfiguration taskConfiguration = new DocumentConfiguration();

        AppConfiguration configuration = new AppConfiguration();
        configuration.setDocuments(documentConfiguration);
        configuration.setTasks(taskConfiguration);

        File test = File.createTempFile("test", ".pdf");
        execute(test, configuration, InputType.DOCUMENT, FileStatus.PROCESSED);
        assertFalse(test.exists(), "File should have been deleted");

        test = File.createTempFile("test", ".pdf");
        execute(test, configuration, InputType.TASK, FileStatus.PROCESSED);
        assertFalse(test.exists(), "File should have been deleted");
    }

    @Test
    void shouldMoveToProcessedFolder() throws IOException {
        DocumentConfiguration documentConfiguration = new DocumentConfiguration();
        DocumentConfiguration taskConfiguration = new DocumentConfiguration();

        File processedFolder = Files.newTemporaryFolder();
        documentConfiguration.setMoveToProcessed(true);
        documentConfiguration.setProcessed(processedFolder);
        taskConfiguration.setMoveToProcessed(true);
        taskConfiguration.setProcessed(processedFolder);

        AppConfiguration configuration = new AppConfiguration();
        configuration.setDocuments(documentConfiguration);
        configuration.setTasks(taskConfiguration);

        File test = File.createTempFile("test", ".pdf");
        execute(test, configuration, InputType.DOCUMENT, FileStatus.PROCESSED);
        assertFalse(test.exists(), "File should have been deleted");
        assertTrue(new File(processedFolder, test.getName()).exists(), "File should have been moved to " + processedFolder);

        test = File.createTempFile("test", ".pdf");
        execute(test, configuration, InputType.TASK, FileStatus.PROCESSED);
        assertFalse(test.exists(), "File should have been deleted");
        assertTrue(new File(processedFolder, test.getName()).exists(), "File should have been moved to " + processedFolder);
    }

    @Test
    void shouldMoveToErrorFolder() throws IOException {
        DocumentConfiguration documentConfiguration = new DocumentConfiguration();
        DocumentConfiguration taskConfiguration = new DocumentConfiguration();

        File errorFolder = Files.newTemporaryFolder();
        documentConfiguration.setMoveToProcessed(true);
        documentConfiguration.setError(errorFolder);
        taskConfiguration.setMoveToProcessed(true);
        taskConfiguration.setError(errorFolder);

        AppConfiguration configuration = new AppConfiguration();
        configuration.setDocuments(documentConfiguration);
        configuration.setTasks(taskConfiguration);

        File test = File.createTempFile("test", ".pdf");
        execute(test, configuration, InputType.DOCUMENT, FileStatus.ERROR);
        assertFalse(test.exists(), "File should have been deleted");
        assertTrue(new File(errorFolder, test.getName()).exists(), "File should have been moved to " + errorFolder);

        test = File.createTempFile("test", ".pdf");
        execute(test, configuration, InputType.TASK, FileStatus.ERROR);
        assertFalse(test.exists(), "File should have been deleted");
        assertTrue(new File(errorFolder, test.getName()).exists(), "File should have been moved to " + errorFolder);
    }

    @Test
    void shouldMoveToErrorFolderOnException() throws IOException {
        DocumentConfiguration documentConfiguration = new DocumentConfiguration();
        DocumentConfiguration taskConfiguration = new DocumentConfiguration();
        ApiConfiguration apiConfiguration = new ApiConfiguration();
        apiConfiguration.setHost("http://localhost");

        File errorFolder = Files.newTemporaryFolder();
        documentConfiguration.setMoveToProcessed(true);
        documentConfiguration.setError(errorFolder);
        taskConfiguration.setMoveToProcessed(true);
        taskConfiguration.setError(errorFolder);

        AppConfiguration configuration = new AppConfiguration();
        configuration.setDocuments(documentConfiguration);
        configuration.setTasks(taskConfiguration);
        configuration.setApi(apiConfiguration);

        File test = File.createTempFile("test", ".pdf");
        new FileSizeCheckingTask(test, InputType.DOCUMENT, configuration, new DummyAvailabilityService()) {
            @Override
            public FileStatus handle(File path) {
                throw new RuntimeException("Test Exception");
            }
        }.run();
        assertFalse(test.exists(), "File should have been deleted");
        assertTrue(new File(errorFolder, test.getName()).exists(), "File should have been moved to " + errorFolder);
    }

    @Test
    void shouldMoveToIgnoredFolder() throws IOException {
        DocumentConfiguration documentConfiguration = new DocumentConfiguration();
        DocumentConfiguration taskConfiguration = new DocumentConfiguration();

        File ignoredFolder = Files.newTemporaryFolder();
        documentConfiguration.setMoveToProcessed(true);
        documentConfiguration.setIgnored(ignoredFolder);
        taskConfiguration.setMoveToProcessed(true);
        taskConfiguration.setIgnored(ignoredFolder);

        AppConfiguration configuration = new AppConfiguration();
        configuration.setDocuments(documentConfiguration);
        configuration.setTasks(taskConfiguration);

        File test = File.createTempFile("test", ".pdf");
        execute(test, configuration, InputType.DOCUMENT, FileStatus.IGNORED);
        assertFalse(test.exists(), "File should have been deleted");
        assertTrue(new File(ignoredFolder, test.getName()).exists(), "File should have been moved to " + ignoredFolder);

        test = File.createTempFile("test", ".pdf");
        execute(test, configuration, InputType.TASK, FileStatus.IGNORED);
        assertFalse(test.exists(), "File should have been deleted");
        assertTrue(new File(ignoredFolder, test.getName()).exists(), "File should have been moved to " + ignoredFolder);
    }

    private void execute(File file, AppConfiguration configuration, InputType taskType, FileStatus fileStatus) {
        new FileSizeCheckingTask(file, taskType, configuration, new DummyAvailabilityService()) {
            @Override
            protected FileStatus handle(File path) {
                return fileStatus;
            }
        }.run();
    }
    
    private static final class DummyAvailabilityService extends ApiAvailabilityService {
        public DummyAvailabilityService() {
            super(null);
        }

        @Override
        public boolean checkAPIAvailability() {
            return true;
        }
    }
}