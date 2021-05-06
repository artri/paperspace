package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.OCRState;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.util.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

public final class TestHelper {
    private TestHelper() {
    }

    public static String rand(String prefix) {
        return prefix + UUID.randomUUID();
    }

    public static String rand() {
        return rand("");
    }

    public static TestFile randPdf() {
        return randPdf(1);
    }

    public static TestFile randPdf(int pages) {
        PDFont font = PDType1Font.HELVETICA_BOLD;

        try (PDDocument doc = new PDDocument()) {
            File target = Files.createTempFile("test_", ".pdf").toFile();
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream contents = new PDPageContentStream(doc, page)) {
                    contents.beginText();
                    contents.setFont(font, 12);
                    contents.newLineAtOffset(100, 700);
                    contents.showText("PAGE " + i + " -TEST: " + UUID.randomUUID());
                    contents.endText();
                }
            }

            doc.save(target);
            InputStream is = new FileInputStream(target);
            return new TestFile(target, DigestUtils.md5DigestAsHex(is));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Binary randBinary(int pages) {
        TestFile testFile = randPdf(pages);
        return new Binary(UUID.randomUUID(), LocalDateTime.now(), testFile.file.getAbsolutePath(), testFile.getHash(), "application/pdf", testFile.file.length(), OCRState.PROCESSED);
    }

    public static Binary randBinary() {
        return randBinary(1);
    }

    public static class TestFile {
        private final File file;
        private final String hash;

        public TestFile(File file, String hash) {
            this.file = file;
            this.hash = hash;
        }

        public File getFile() {
            return file;
        }

        public String getHash() {
            return hash;
        }
    }
}
