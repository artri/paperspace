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


    public FileTypeTaskDispatcher(File file, InputType inputType, AppConfiguration configuration, ExecutorService threadPool) {
        this.file = file;
        this.inputType = inputType;
        this.configuration = configuration;
        this.threadPool = threadPool;
    }

    @Override
    public void run() {
        boolean writable = waitForFileToBeCompletelyWritten();
        if (writable) {
            log.debug("file [{}] fully written, determine file type now", file);
            FileSizeCheckingTask fileHandlerTask = scheduleFileHandlerTask();
            log.info("dispatching file [{}] to new task of type [{}]", file, fileHandlerTask.getClass());
            this.threadPool.submit(fileHandlerTask);
        } else {
            this.threadPool.submit(new MoveToErrorPath(file, inputType, configuration));
        }
    }

    private boolean waitForFileToBeCompletelyWritten() {
        log.debug("checking file size of [{}]", file);
        long lastCheckedSize = -1L;
        int round = 1;
        while (round++ <= 20 && lastCheckedSize != file.length()) {
            log.debug("run [{}]: lastCheckedSize was [{}] current size now is [{}]", round, lastCheckedSize, file.length());
            try {
                Thread.sleep(5000);
                lastCheckedSize = file.length();
            } catch (InterruptedException ignored) {
            }
        }
        return lastCheckedSize == file.length();
    }

    private FileSizeCheckingTask scheduleFileHandlerTask() {
        try {
            System.out.println(file.length());
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
