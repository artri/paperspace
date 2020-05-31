package com.dedicatedcode.paperspace;

import java.io.IOException;

public class StorageException extends RuntimeException {
    public StorageException(IOException e) {
        super(e);
    }
}
