package com.dedicatedcode.paperspace.modifiers;

import com.dedicatedcode.paperspace.ModificationService;
import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.web.PageEditModel;
import com.dedicatedcode.paperspace.web.PageEditTransformation;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PdfBinaryModifier implements ModificationService {
    private static final Logger log = LoggerFactory.getLogger(PdfBinaryModifier.class);
    private final String staplerPath;

    public PdfBinaryModifier(@Value("${modifiers.pdf.path}") String staplerPath) {
        this.staplerPath = staplerPath;
    }

    @Override
    public boolean isEnabled() {
        try {
            Process process = Runtime.getRuntime().exec(staplerPath);
            int status = process.waitFor();
            return status == 0 || status == 2;
        } catch (IOException | InterruptedException e) {
            log.warn("Could not execute stapler. Will disable PdfBinaryModifier.");
            return false;
        }
    }

    @Override
    public List<String> supportedFileFormats() {
        return Collections.singletonList("application/pdf");
    }

    @Override
    public Binary modify(Binary binary, List<PageEditModel> oldVersion, List<PageEditModel> updatedVersion) {
        try {
            String source = binary.getStorageLocation();
            List<String> pageSelection = updatedVersion.stream().filter(pageEditModel -> !pageEditModel.getTransformations().contains(PageEditTransformation.DELETE)).map(pageEditModel -> {

                int rotationDirection = 0;
                for (PageEditTransformation transformation : pageEditModel.getTransformations()) {
                    if (transformation == PageEditTransformation.ROTATE_CLOCKWISE) {
                        rotationDirection++;
                    } else if (transformation == PageEditTransformation.ROTATE_COUNTER_CLOCKWISE) {
                        rotationDirection--;
                    }
                }

                String rotation = "";
                switch (rotationDirection % 4) {
                    case 0:
                        break;
                    case 1:
                    case -3:
                        rotation = "R";
                        break;
                    case 2:
                    case -2:
                        rotation = "D";
                        break;
                    case 3:
                    case -1:
                        rotation = "L";
                }
                return pageEditModel.getPage().getNumber() + rotation;
            }).collect(Collectors.toList());


            pageSelection.add(0, staplerPath);
            pageSelection.add(1, "sel");
            pageSelection.add(2, source);
            String targetPath = System.getProperty("java.io.tmpdir") + File.separatorChar + "pdf_modified_" + binary.getId() + "_" + System.currentTimeMillis() + ".pdf";
            pageSelection.add(targetPath);
            log.debug("Executing process [{}]", String.join(" ", pageSelection));
            Process process = Runtime.getRuntime().exec(pageSelection.toArray(new String[0]));
            int statusCode = process.waitFor();

            if (statusCode == 0) {
                log.debug("New version of pdf [{}] created. Will overwrite current binary.", targetPath);
                IOUtils.copy(new FileInputStream(targetPath), new FileOutputStream(source));
            } else {
                log.error("Process returned status code [{}]", statusCode);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Could not modify binary [{}]", binary.getId());
            return null;
        }
        return null;
    }
}
