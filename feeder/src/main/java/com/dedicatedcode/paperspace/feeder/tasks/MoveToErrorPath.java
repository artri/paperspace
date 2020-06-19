package com.dedicatedcode.paperspace.feeder.tasks;

import com.dedicatedcode.paperspace.feeder.FileStatus;
import com.dedicatedcode.paperspace.feeder.InputType;
import com.dedicatedcode.paperspace.feeder.configuration.AppConfiguration;

import java.io.File;

public class MoveToErrorPath extends FileSizeCheckingTask {
    public MoveToErrorPath(File file, InputType inputType, AppConfiguration configuration, ApiAvailabilityService availabilityService) {
        super(file, inputType, configuration, availabilityService);
    }

    @Override
    protected FileStatus handle(File path) {
        return FileStatus.ERROR;
    }
}
