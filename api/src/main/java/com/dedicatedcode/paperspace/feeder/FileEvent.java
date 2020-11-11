package com.dedicatedcode.paperspace.feeder;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Objects;

class FileEvent {
    private final InputType inputType;
    private final EventType eventType;
    private final File file;
    private final LocalDateTime occurredAt;
    private final String hash;

    FileEvent(InputType inputType, EventType eventType, File file, LocalDateTime occurredAt, String hash) {
        this.inputType = inputType;
        this.eventType = eventType;
        this.file = file;
        this.occurredAt = occurredAt;
        this.hash = hash;
    }

    public EventType getEventType() {
        return eventType;
    }

    public File getFile() {
        return file;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getHash() {
        return hash;
    }

    public InputType getInputType() {
        return inputType;
    }

    @Override
    public String toString() {
        return "FileEvent{" +
                "inputType=" + inputType +
                ", eventType=" + eventType +
                ", file=" + file +
                ", occurredAt=" + occurredAt +
                ", hash='" + hash + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileEvent fileEvent = (FileEvent) o;
        return Objects.equals(file, fileEvent.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file);
    }
}
