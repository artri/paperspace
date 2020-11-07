package com.dedicatedcode.paperspace.feeder;

import com.dedicatedcode.paperspace.BinaryService;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FolderWatchService {

    private static final Logger log = LoggerFactory.getLogger(FolderWatchService.class);
    private final ExecutorService threadPool;
    private final File documentsPath;
    private final File tasksPath;
    private final long folderCheckInterval;
    private final BinaryService binaryService;
    private final FileAlterationMonitor fileMonitor;
    private final MergingFileEventHandler fileEventHandler;

    @Autowired
    public FolderWatchService(ExecutorService threadPool,
                              @Value("${storage.folder.documents}") File documentsPath,
                              @Value("${storage.folder.tasks}") File tasksPath,
                              @Value("${storage.folder.check_interval}") long folderCheckInterval,
                              BinaryService binaryService,
                              MergingFileEventHandler fileEventHandler) {
        this.threadPool = threadPool;
        this.documentsPath = documentsPath;
        this.tasksPath = tasksPath;
        this.folderCheckInterval = folderCheckInterval;
        this.binaryService = binaryService;
        this.fileEventHandler = fileEventHandler;
        this.fileMonitor = registerWatchServices();
    }

    @PostConstruct
    public void checkExistingBinaries() {
        this.binaryService.getAll().stream()
                .filter(binary -> binary.getStorageLocation().startsWith(documentsPath.getAbsolutePath()) || binary.getStorageLocation().startsWith(tasksPath.getAbsolutePath()))
                .filter(binary -> !new File(binary.getStorageLocation()).exists())
                .forEach(binary -> fileEventHandler.register(EventType.DELETE, new File(binary.getStorageLocation()), binary.getStorageLocation().startsWith(documentsPath.getAbsolutePath()) ? InputType.DOCUMENT : InputType.TASK));
    }

    private FileAlterationMonitor registerWatchServices() {
        FileAlterationMonitor monitor = new FileAlterationMonitor(folderCheckInterval);
        if (tasksPath != null) {
            createFolderIfMissing(tasksPath, InputType.TASK);
            FileAlterationObserver observer = new FileAlterationObserver(tasksPath);
            FileAlterationListenerAdaptor fileChangeListener = new TypeAwareFileAlterationListener(fileEventHandler, tasksPath, InputType.TASK);
            observer.addListener(fileChangeListener);
            monitor.addObserver(observer);
        }
        if (documentsPath != null) {
            createFolderIfMissing(documentsPath, InputType.DOCUMENT);
            FileAlterationObserver observer = new FileAlterationObserver(documentsPath);
            FileAlterationListenerAdaptor fileChangeListener = new TypeAwareFileAlterationListener(fileEventHandler, documentsPath, InputType.DOCUMENT);
            observer.addListener(fileChangeListener);
            monitor.addObserver(observer);
        }
        try {
            monitor.start();
        } catch (Exception e) {
            throw new RuntimeException("Unable to start file monitor", e);
        }

        return monitor;
    }

    private void createFolderIfMissing(File tasksPath, InputType type) {
        if (tasksPath.exists()) {
            if (!tasksPath.isDirectory()) {
                throw new RuntimeException("given path [" + tasksPath + "] for type [" + type + "] is not a directory, will exit");
            } else {
                log.debug("directory [{}] for [{}] exists", tasksPath, type);
            }
        } else {
            log.info("creating directory [{}] for type [{}]", tasksPath, type);
            if (!tasksPath.mkdirs()) {
                throw new RuntimeException("could not create path [" + tasksPath + "] for type [" + type + "], will exit");
            }
        }
    }

    @PreDestroy
    public void stop() {
        this.threadPool.shutdownNow();
        try {
            this.fileMonitor.stop();
        } catch (Exception e) {
            log.error("unable to stop file monitor", e);
        }
    }

    private static class TypeAwareFileAlterationListener extends FileAlterationListenerAdaptor {
        private final MergingFileEventHandler fileEventHandler;
        private final InputType type;

        public TypeAwareFileAlterationListener(MergingFileEventHandler fileEventHandler, File path, InputType type) {
            this.fileEventHandler = fileEventHandler;
            this.type = type;
            handleExistingFiles(path, type);
        }

        @Override
        public void onDirectoryCreate(File directory) {
        }

        @Override
        public void onDirectoryChange(File directory) {
        }

        @Override
        public void onFileCreate(File file) {
            log.debug("[CREATE] file [{}]", file);
            fileEventHandler.register(EventType.CREATE, file, type);
            }

        @Override
        public void onFileChange(File file) {
            log.debug("[CHANGE] file [{}]", file);
            fileEventHandler.register(EventType.CHANGE, file, type);
        }

        @Override
        public void onFileDelete(File file) {
            log.debug("[DELETION] file [{}]", file);
            fileEventHandler.register(EventType.DELETE, file, type);
        }

        private void handleExistingFiles(File folder, InputType type) {
            try {
                AtomicInteger count = new AtomicInteger();
                Files.walk(folder.toPath(), FileVisitOption.FOLLOW_LINKS).forEach(path -> {
                    File existingFile = path.toFile();
                    count.getAndIncrement();
                    if (!existingFile.isDirectory()) {
                        log.debug("found existing file [{}]", existingFile);
                        fileEventHandler.register(EventType.EXISTING, existingFile, type);
                    }
                });
            } catch (IOException e) {
                log.error("checking existing folder runs into:", e);
            }
        }
    }
}
