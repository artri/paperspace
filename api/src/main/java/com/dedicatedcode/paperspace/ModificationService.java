package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.web.PageEditModel;

import java.util.List;

public interface ModificationService {

    boolean isEnabled();

    List<String> supportedFileFormats();

    boolean modify(Binary binary, List<PageEditModel> oldVersion, List<PageEditModel> updatedVersion);
}
