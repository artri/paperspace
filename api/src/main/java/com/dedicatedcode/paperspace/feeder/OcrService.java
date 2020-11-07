package com.dedicatedcode.paperspace.feeder;

import com.dedicatedcode.paperspace.model.Page;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

public interface OcrService {
    List<Page> doOcr(File inputFile) throws OcrException;
}
