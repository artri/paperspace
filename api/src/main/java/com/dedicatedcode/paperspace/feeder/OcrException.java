package com.dedicatedcode.paperspace.feeder;

public class OcrException extends Exception {
    public OcrException(String message) {
        super(message);
    }

    public OcrException(Throwable cause) {
        super(cause);
    }
}
