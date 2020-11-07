package com.dedicatedcode.paperspace.feeder;

import com.dedicatedcode.paperspace.StorageService;
import com.dedicatedcode.paperspace.UploadType;
import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.Page;
import com.recognition.software.jdeskew.ImageDeskew;
import net.coobird.thumbnailator.Thumbnails;
import net.sf.jmimemagic.*;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.ImageHelper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PdfOcrService implements OcrService {
    private static final Logger log = LoggerFactory.getLogger(PdfOcrService.class);
    private static final double MINIMUM_DESKEW_THRESHOLD = 0.05d;

    private final StorageService storageService;
    private final String ocrLanguage;
    private final String ocrDataPath;

    public PdfOcrService(StorageService storageService, @Value("${ocr.language}") String ocrLanguage, @Value("${ocr.datapath}") String ocrDataPath) {
        this.storageService = storageService;
        this.ocrLanguage = ocrLanguage;
        this.ocrDataPath = ocrDataPath;
    }

    @Override
    public List<Page> doOcr(File inputFile) throws OcrException {
        try (PDDocument document = PDDocument.load(inputFile)) {
            List<Page> pages = new ArrayList<>();
            PDFTextStripper textStripper = new PDFTextStripper();
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int pageCounter = 0;
            for (PDPage ignored : document.getPages()) {
                textStripper.setStartPage(pageCounter);
                BufferedImage originalPageImage = pdfRenderer.renderImageWithDPI(pageCounter, 300, ImageType.RGB);

                pageCounter += 1;
                textStripper.setEndPage(pageCounter);
                String pageText = textStripper.getText(document);
                if (!StringUtils.isEmpty(pageText) && !pageText.replace("\n", "").isEmpty()) {
                    log.debug("page contains text, will skip ocr run");
                } else {
                    log.debug("could not extract text of page, will submit to ocr process");
                    ImageDeskew id = new ImageDeskew(originalPageImage);
                    double imageSkewAngle = id.getSkewAngle();
                    BufferedImage ocrImage;
                    if ((imageSkewAngle > MINIMUM_DESKEW_THRESHOLD || imageSkewAngle < -(MINIMUM_DESKEW_THRESHOLD))) {
                        log.debug("image skew angle was [{}] which is over [{}] allowed, will deskew the image", imageSkewAngle, MINIMUM_DESKEW_THRESHOLD);
                        ocrImage = ImageHelper.rotateImage(originalPageImage, -imageSkewAngle);
                    } else {
                        ocrImage = originalPageImage;
                    }
                    log.debug("starting ocr ...");
                    Tesseract tesseract = new Tesseract();
                    try {
                        tesseract.setLanguage(this.ocrLanguage);
                        tesseract.setDatapath(this.ocrDataPath);
                        pageText = tesseract.doOCR(ocrImage);
                    } catch (TesseractException e) {
                        log.warn("could not extract content of page for [{}], will pretend no text", inputFile, e);
                        pageText = null;
                    }
                    log.debug("ocr finished");
                }

                File previewImageFile = File.createTempFile("paper{s}pace-preview", ".png");
                log.debug("writing preview image for page to [{}]", previewImageFile);
                if (writeImageFile(originalPageImage, "png", previewImageFile)) {
                    String mimeType = loadMimeType(previewImageFile);
                    Binary imageBinary = this.storageService.store(UploadType.IMAGE, previewImageFile, "ps_preview_image_" + UUID.randomUUID() + ".png", mimeType);
                    pages.add(new Page(UUID.randomUUID(), pageCounter, pageText, imageBinary));
                }
            }
            return pages;
        } catch (IOException e) {
            throw new OcrException(e);
        }
    }

    private boolean writeImageFile(BufferedImage image, String format, File previewImageFile) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();

        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);

        //adding metadata
        IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);

        IIOMetadataNode textEntry = new IIOMetadataNode("tEXtEntry");
        textEntry.setAttribute("keyword", "created");
        textEntry.setAttribute("value", LocalDateTime.now().toString());

        IIOMetadataNode text = new IIOMetadataNode("tEXt");
        text.appendChild(textEntry);

        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_png_1.0");
        root.appendChild(text);

        metadata.mergeTree("javax_imageio_png_1.0", root);

        //writing the data
        FileOutputStream baos = new FileOutputStream(previewImageFile);
        ImageOutputStream stream = ImageIO.createImageOutputStream(baos);
        writer.setOutput(stream);
        writer.write(metadata, new IIOImage(Thumbnails.of(image).scale(0.5).asBufferedImage(), null, metadata), writeParam);
        stream.close();
        return true;
    }
    private String loadMimeType(File path) {
        try {
            MagicMatch magicMatch = Magic.getMagicMatch(path, true, false);
            return magicMatch.getMimeType();
        } catch (MagicParseException | MagicMatchNotFoundException | MagicException e) {
            log.warn("could not extract mimeType of [{}], will assume octet-stream", path);
            return "application/octet-stream";
        }
    }
}
