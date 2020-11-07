package com.dedicatedcode.paperspace.feeder;

import java.io.File;

public interface FileEventListener {

    void handle(EventType eventType, File file, InputType inputType);
}
