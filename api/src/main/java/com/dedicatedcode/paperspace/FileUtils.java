package com.dedicatedcode.paperspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public final class FileUtils {
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    private FileUtils() {
    }

    public static void clearDirectory(File directory) {
        try {
            if (directory.exists()) {
                org.apache.commons.io.FileUtils.deleteDirectory(directory);
            }
            org.apache.commons.io.FileUtils.forceMkdir(directory);
        } catch (IOException e) {
            log.error("could not clear directory");
        }
    }
}
