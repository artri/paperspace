package com.dedicatedcode.paperspace.feeder;

import com.dedicatedcode.paperspace.feeder.configuration.AppConfiguration;
import com.dedicatedcode.paperspace.feeder.tasks.FileTypeTaskDispatcher;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

@Service
public class Feeder {
    private static final Logger log = LoggerFactory.getLogger(Feeder.class);

    private final ExecutorService threadPool;
    private final ExecutorService watcherThreadPool;
    private final AppConfiguration configuration;

    @Autowired
    public Feeder(ExecutorService fileHandlerThreadPool, ExecutorService watcherThreadPool, AppConfiguration configuration) {
        this.threadPool = fileHandlerThreadPool;
        this.watcherThreadPool = watcherThreadPool;
        this.configuration = configuration;
        Arrays.asList(
                this.configuration.getTasks().getInput(),
                this.configuration.getTasks().getIgnored(),
                this.configuration.getTasks().getError(),
                this.configuration.getTasks().getProcessed(),
                this.configuration.getDocuments().getInput(),
                this.configuration.getDocuments().getIgnored(),
                this.configuration.getDocuments().getError(),
                this.configuration.getDocuments().getProcessed())
                .forEach(this::createIfNecessary);
    }

    private void createIfNecessary(File path) {
        if (path.isDirectory()) {
            log.debug("folder exists [{}]", path);
        } else {
            try {
                FileUtils.forceMkdir(path);
                log.info("created folder [{}] ", path);
            } catch (IOException e) {
                log.error("Could not create folder [{}] and folder does not exists. Will stop startup!", path);
                throw new RuntimeException(e);
            }
        }
    }

    private Callable<Void> createWatchTask(File folderToWatch, InputType type) {
        return () -> {
            try {
                log.info("starting to watch for changes in [{}]", folderToWatch);
                WatchService watchService = FileSystems.getDefault().newWatchService();
                folderToWatch.toPath().register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);

                WatchKey key;
                while ((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                            Path filePath = (Path) event.context();
                            File newlyCreatedFile = new File(folderToWatch, filePath.toString());
                            log.debug("new file [{}] detected, scheduling file detector", newlyCreatedFile);
                            threadPool.submit(new FileTypeTaskDispatcher(newlyCreatedFile, type, configuration, threadPool));
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException | IOException ex) {
                log.info("WatchService is interrupted");
                return null;
            }
            throw new RuntimeException("should not be reached");
        };
    }

    @PostConstruct
    private void registerWatchServices() {
        handleExistingFiles(this.configuration.getTasks().getInput(), InputType.TASK);
        handleExistingFiles(this.configuration.getDocuments().getInput(), InputType.DOCUMENT);
        List<Callable<Void>> watchers = new ArrayList<>();
        watchers.add(createWatchTask(this.configuration.getTasks().getInput(), InputType.TASK));
        watchers.add(createWatchTask(this.configuration.getDocuments().getInput(), InputType.DOCUMENT));

        try {
            this.watcherThreadPool.invokeAll(watchers);
        } catch (InterruptedException e) {
            log.info("WatchService is interrupted");
        }
    }

    private void handleExistingFiles(File folder, InputType type) {
        File[] existingTasks = folder.listFiles();
        if (existingTasks != null) {
            Arrays.asList(existingTasks)
                    .forEach(file -> threadPool.submit(new FileTypeTaskDispatcher(file, type, configuration, threadPool)));
        }
    }
}
