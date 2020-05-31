package com.dedicatedcode.paperspace.feeder.tasks;

import com.dedicatedcode.paperspace.feeder.FileStatus;
import com.dedicatedcode.paperspace.feeder.InputType;
import com.dedicatedcode.paperspace.feeder.configuration.AppConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class FileSizeCheckingTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(FileSizeCheckingTask.class);
    private final File file;
    final InputType inputType;
    final AppConfiguration configuration;

    FileSizeCheckingTask(File file, InputType inputType, AppConfiguration configuration) {
        this.file = file;
        this.inputType = inputType;
        this.configuration = configuration;
    }

    @Override
    public final void run() {

        try {
            FileStatus fileStatus = handle(file);
            log.info("File [{}] handling results in [{}]", file, fileStatus);
            boolean shouldMoveProcessedFile = fileStatus != FileStatus.PROCESSED || this.configuration.getConfigurationBy(inputType).isMoveToProcessed();
            if (shouldMoveProcessedFile) {
                Path targetPath = getTargetFolder(inputType, fileStatus).resolve(file.getName());
                log.info("file [{}] will be moved to [{}]", file, targetPath);
                Files.move(file.toPath(), targetPath);
            } else {
                log.info("file [{}] will be deleted since it is processed and should not be moved to archive folder", file);
                Files.deleteIfExists(file.toPath());
            }
        } catch (Exception e) {
            log.error("error occurred while handling file [{}]", file, e);
            try {
                Path targetPath = getTargetFolder(inputType, FileStatus.ERROR).resolve(file.getName());
                log.info("file [{}] will be moved to [{}]", file, targetPath);
                Files.move(file.toPath(), targetPath);
            } catch (IOException ioException) {
                log.info("could not move [{}] to error folder", file, ioException);
            }
        }
    }

    private Path getTargetFolder(InputType inputType, FileStatus fileStatus) {
        switch (fileStatus) {
            case PROCESSED:
                return this.configuration.getConfigurationBy(inputType).getProcessed().toPath();
            case ERROR:
                return this.configuration.getConfigurationBy(inputType).getError().toPath();
            case IGNORED:
                return this.configuration.getConfigurationBy(inputType).getIgnored().toPath();
            default:
                throw new RuntimeException("unhandled file status [" + fileStatus + "]");
        }
    }

    protected abstract FileStatus handle(File path) throws IOException;
}
