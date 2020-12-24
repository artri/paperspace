package com.dedicatedcode.paperspace.feeder;

import com.dedicatedcode.paperspace.BinaryService;
import com.dedicatedcode.paperspace.TestHelper;
import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.OCRState;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class MergingFileEventHandlerTest {

    @Value("${storage.folder.documents}")
    private File documentStorage;

    @Autowired
    private BinaryService binaryService;

    @MockBean
    private PdfOcrService pdfOcrService;

    @BeforeEach
    void setUp() throws OcrException {
        when(pdfOcrService.supportedFileFormats()).thenReturn(Collections.singletonList("application/pdf"));
        when(pdfOcrService.doOcr(any(File.class))).thenReturn(Collections.emptyList());
    }

    @Test
    void shouldHandleDuplicatedFileIfAlreadyKnown() throws IOException, InterruptedException {
        File targetFile = new File(documentStorage, UUID.randomUUID() + ".pdf");
        TestHelper.TestFile testFile = TestHelper.randPdf();
        FileUtils.moveFile(testFile.getFile(), targetFile);

        await(testFile);

        File copy = new File(documentStorage, targetFile.getName() + "_duplicate");
        FileUtils.copyFile(targetFile, copy);


        while (this.binaryService.getAll().stream().noneMatch(binary -> binary.getStorageLocation().equals(copy.getAbsolutePath()))) {
            Thread.sleep(500);
        }

        Optional<Binary> storedBinary = this.binaryService.getAll().stream().filter(binary -> binary.getStorageLocation().equals(copy.getAbsolutePath())).findFirst();
        assertTrue(storedBinary.isPresent());
        assertEquals(OCRState.DUPLICATE, storedBinary.get().getState());
    }

    private void await(TestHelper.TestFile testFile) throws InterruptedException {
        while (this.binaryService.getAll().stream().noneMatch(binary -> binary.getHash().equals(testFile.getHash()))) {
            Thread.sleep(500);
        }
    }
}