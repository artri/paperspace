package com.dedicatedcode.paperspace.feeder.configuration;

import java.io.File;

public class DocumentConfiguration {
    private File input;
    private File ignored;
    private File error;
    private File processed;
    private boolean moveToProcessed;

    public File getInput() {
        return input;
    }

    public void setInput(File input) {
        this.input = input;
    }

    public File getIgnored() {
        return ignored;
    }

    public void setIgnored(File ignored) {
        this.ignored = ignored;
    }

    public File getError() {
        return error;
    }

    public void setError(File error) {
        this.error = error;
    }

    public File getProcessed() {
        return processed;
    }

    public void setProcessed(File processed) {
        this.processed = processed;
    }

    public boolean isMoveToProcessed() {
        return moveToProcessed;
    }

    public void setMoveToProcessed(boolean moveToProcessed) {
        this.moveToProcessed = moveToProcessed;
    }
}
