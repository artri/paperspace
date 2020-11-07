package com.dedicatedcode.paperspace.feeder;

import com.dedicatedcode.paperspace.BinaryService;
import com.dedicatedcode.paperspace.model.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.dedicatedcode.paperspace.feeder.EventType.*;

@Service
public class MergingFileEventHandler {
    private static final Logger log = LoggerFactory.getLogger(MergingFileEventHandler.class);
    private final BinaryService binaryService;
    private final List<FileEventListener> listeners;
    private final Map<FileEvent, Long> recheckList = new HashMap<>();
    private final long recheckInterval;
    private final long notifyInterval;

    private final List<FileEvent> queuedFileEvents = Collections.synchronizedList(new LinkedList<>());

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final ExecutorService ocrExecutor = Executors.newFixedThreadPool(8);
    private final AtomicInteger executingCount =new  AtomicInteger();
    @Autowired
    public MergingFileEventHandler(BinaryService binaryService, List<FileEventListener> listeners,
                                   @Value("${storage.recheck.interval}") long recheckInterval,
                                   @Value("${storage.notify.interval}") long notifyInterval) {
        this.binaryService = binaryService;
        this.listeners = listeners;
        this.recheckInterval = recheckInterval;
        this.notifyInterval = notifyInterval;
        initThreads();
    }

    private void initThreads() {
        executor.scheduleWithFixedDelay(() -> {
            LocalDateTime now = LocalDateTime.now().minus(recheckInterval, ChronoUnit.MILLIS);
            List<FileEvent> toHandle = new ArrayList<>();
            HashMap<FileEvent, Long> map;
            synchronized (recheckList) {
                map = new HashMap<>(recheckList);
                recheckList.clear();
            }
            map.forEach((event, value) -> {
                if (event.getOccurredAt().isBefore(now)) {
                    log.debug("checking file size of [{}]", event.getFile());
                    if (event.getFile().length() == value) {
                        register(event.getEventType(), event.getFile(), event.getInputType());
                    } else {
                        toHandle.add(event);
                    }
                }
            });
            synchronized (recheckList) {
                toHandle.forEach(fileEvent -> recheckList.put(fileEvent, fileEvent.getFile().length()));
            }

        }, 0, recheckInterval, TimeUnit.MILLISECONDS);

        executor.scheduleWithFixedDelay(() -> {
            LocalDateTime now = LocalDateTime.now().minus(notifyInterval, ChronoUnit.MILLIS);
            synchronized (this.queuedFileEvents) {
                long countBefore = this.queuedFileEvents.size();
                Iterator<FileEvent> iterator = this.queuedFileEvents.listIterator();
                int notifiedCount = 0;
                while (iterator.hasNext()) {
                    FileEvent fileEvent = iterator.next();
                    if (fileEvent.getOccurredAt().isBefore(now)) {
                        notifiedCount++;
                        iterator.remove();
                        executingCount.incrementAndGet();
                        this.ocrExecutor.submit(() -> {
                            try {
                                notifyListeners(fileEvent);
                            } catch (Exception e) {
                                log.error("error occurred while handling [{}] will put it back to retry.", fileEvent, e);
                                this.recheckList.put(fileEvent, fileEvent.getFile().length());
                            }
                            executingCount.decrementAndGet();
                        });
                    }
                }
                log.debug("notified about changes on [{}] files out of [{}] events, pending changes [{}]", notifiedCount, countBefore, this.queuedFileEvents.size());
            }



        }, 0, notifyInterval, TimeUnit.MILLISECONDS);
    }

    private void notifyListeners(FileEvent fileEvent) {
        for (FileEventListener listener : listeners) {
            log.debug("notifying [{}] with file [{}] and event [{}]", listener.getClass(), fileEvent.getFile(), fileEvent.getEventType());
            listener.handle(fileEvent.getEventType(), fileEvent.getFile(), fileEvent.getInputType());
        }
    }

    public void register(EventType eventType, File file, InputType inputType) {
        String hash = null;
        switch (eventType) {
            case DELETE:
                hash = getKnownHashFor(file);
                break;
            case CREATE:
            case EXISTING:
            case CHANGE:
                String fileHash = getHashFrom(file);
                if (fileHash == null) {
                    synchronized (recheckList) {
                        recheckList.put(new FileEvent(inputType, eventType, file, LocalDateTime.now(), null), file.length());
                    }
                } else {
                    hash = fileHash;
                }
                break;
            default:
                throw new IllegalArgumentException("Unable to handle event type [" + eventType + "]");
        }

        if (hash != null) {
            log.info("registered event for file [{}] with hash [{}]", file, hash);
            String finalHash = hash;
            synchronized (queuedFileEvents) {
                List<FileEvent> updatedList = this.queuedFileEvents.stream().filter(event -> !event.getHash().equals(finalHash) && !event.getFile().equals(file)).collect(Collectors.toList());
                FileEvent storedEventByHash = this.queuedFileEvents.stream().filter(event -> event.getHash().equals(finalHash)).findFirst().orElse(null);
                FileEvent storedEventByFileName = this.queuedFileEvents.stream().filter(event -> event.getFile().equals(file)).findFirst().orElse(null);

                log.debug("Found existing event by hash [{}] or fileName [{}] for file[{}]", storedEventByHash, storedEventByFileName, file);
                FileEvent storedEvent = storedEventByHash != null ? storedEventByHash : storedEventByFileName;
                if (storedEvent != null) {
                    EventType storedType = storedEvent.getEventType();
                    if (storedType == DELETE && eventType == CREATE) {
                        log.info("DELETE event stored for [{}] and CREATE event retrieved for [{}]. Merge to MOVE type", storedEvent.getFile(), file);
                        updatedList.add(new FileEvent(inputType, MOVE, file, LocalDateTime.now(), hash));
                    } else if ((storedType == CREATE || storedType == EXISTING) && eventType == DELETE) {
                        log.info("CREATE event stored for [{}] and DELETE event retrieved for [{}]. Merge to MOVE type", storedEvent.getFile(), file);
                        updatedList.add(new FileEvent(inputType, MOVE, storedEvent.getFile(), LocalDateTime.now(), hash));
                    } else if (eventType == CHANGE) {
                        log.info("CHANGED event received for [{}] and [{}] event retrieved for [{}]. Will update time", file, storedEvent.getEventType(), storedEvent.getFile());
                        updatedList.add(new FileEvent(inputType, storedEvent.getEventType(), file, LocalDateTime.now(), hash));
                    } else if (storedType == EXISTING) {
                        FileEvent fileEvent = new FileEvent(inputType, eventType, file, LocalDateTime.now(), hash);
                        log.info("Event [{}] received and stored event was [{}]", fileEvent, storedEvent);
                        updatedList.add(fileEvent);
                    }
                } else {
                    FileEvent fileEvent = new FileEvent(inputType, eventType, file, LocalDateTime.now(), hash);
                    updatedList.add(fileEvent);
                    log.info("Event [{}] received", fileEvent);

                }
                this.queuedFileEvents.clear();
                this.queuedFileEvents.addAll(updatedList);
            }
        }
    }

    private String getHashFrom(File file) {
        try (InputStream is = new FileInputStream(file)) {
            return DigestUtils.md5DigestAsHex(is);
        } catch (IOException e) {
            log.error("could not calculate md5 sum of [{}]", file);
            return null;
        }
    }

    private String getKnownHashFor(File file) {
        Binary byPath = this.binaryService.getByPath(file.getAbsolutePath());
        return byPath != null ? byPath.getHash() : null;
    }

    public int getPendingChanges() {
        return this.queuedFileEvents.size() + this.executingCount.get();
    }
}
