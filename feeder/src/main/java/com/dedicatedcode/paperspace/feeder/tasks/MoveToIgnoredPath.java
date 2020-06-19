package com.dedicatedcode.paperspace.feeder.tasks;

import com.dedicatedcode.paperspace.feeder.FileStatus;
import com.dedicatedcode.paperspace.feeder.InputType;
import com.dedicatedcode.paperspace.feeder.configuration.AppConfiguration;

import java.io.File;

public class MoveToIgnoredPath extends FileSizeCheckingTask {
    public MoveToIgnoredPath(File file, InputType inputType, AppConfiguration configuration, ApiAvailabilityService availabilityService) {
        super(file, inputType, configuration, availabilityService);
    }

    @Override
    protected FileStatus handle(File path) {
        return FileStatus.IGNORED;
    }
}
