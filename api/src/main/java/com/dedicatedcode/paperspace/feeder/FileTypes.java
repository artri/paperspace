package com.dedicatedcode.paperspace.feeder;

import net.sf.jmimemagic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class FileTypes {
    private static final Logger log = LoggerFactory.getLogger(FileTypes.class);

    public static String loadMimeType(File path) {
        try {
            MagicMatch magicMatch = Magic.getMagicMatch(path, true, false);
            return magicMatch.getMimeType();
        } catch (MagicParseException | MagicMatchNotFoundException | MagicException e) {
            log.warn("could not extract mimeType of [{}], will assume octet-stream", path);
            return "application/octet-stream";
        }
    }
}
