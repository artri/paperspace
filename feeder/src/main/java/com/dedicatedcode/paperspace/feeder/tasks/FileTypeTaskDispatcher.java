package com.dedicatedcode.paperspace.feeder.tasks;

import com.dedicatedcode.paperspace.feeder.InputType;
import com.dedicatedcode.paperspace.feeder.configuration.AppConfiguration;
import net.sf.jmimemagic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ExecutorService;

public class FileTypeTaskDispatcher implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(FileTypeTaskDispatcher.class);
    private final File file;
    private final InputType inputType;
    private final AppConfiguration configuration;
    private final ExecutorService threadPool;
    private long lastFileSize = -1;


    public FileTypeTaskDispatcher(File file, InputType inputType, AppConfiguration configuration, ExecutorService threadPool) {
        this.file = file;
        this.inputType = inputType;
        this.configuration = configuration;
        this.threadPool = threadPool;
    }

    @Override
    public void run() {
        long currentSize = file.length();
        while (currentSize != lastFileSize && currentSize == 0) {
            log.debug("file [{}] is still written size!=lastCheckedSize [{}!={}], will sleep for a second", file, currentSize, lastFileSize);
            lastFileSize = currentSize;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("interrupted", e);
            }
        }
        log.debug("file [{}] fully written, determine file type now", file);
        FileSizeCheckingTask fileHandlerTask = scheduleFileHandlerTask();
        log.info("dispatching file [{}] to new task of type [{}]", file, fileHandlerTask.getClass());
        this.threadPool.submit(fileHandlerTask);
    }

    private FileSizeCheckingTask scheduleFileHandlerTask() {
        try {
            MagicMatch magicMatch = Magic.getMagicMatch(file, true, false);
            String mimeType = magicMatch.getMimeType();
            if ("application/pdf".equals(mimeType)) {
                return new PDFDocumentHandler(file, inputType, configuration);
            } else {
                return new MoveToIgnoredPath(file, inputType, configuration);
            }
        } catch (MagicParseException | MagicMatchNotFoundException | MagicException e) {
            log.warn("getting file type of [{}] runs into", file, e);
            return new MoveToErrorPath(file, inputType, configuration);
        }
    }
}
