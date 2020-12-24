package com.dedicatedcode.paperspace.feeder;

import com.dedicatedcode.paperspace.StorageService;
import com.dedicatedcode.paperspace.UploadType;
import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.Page;
import com.recognition.software.jdeskew.ImageDeskew;
import net.coobird.thumbnailator.Thumbnails;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.ImageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ImageOcrService implements OcrService {
    private static final Logger log = LoggerFactory.getLogger(ImageOcrService.class);
    private static final double MINIMUM_DESKEW_THRESHOLD = 0.05d;

    private final StorageService storageService;
    private final String ocrLanguage;
    private final String ocrDataPath;
    private static final List<String> SUPPORTED_FILE_FORMATS = List.of("image/jpeg", "image/png");

    public ImageOcrService(StorageService storageService, @Value("${ocr.language}") String ocrLanguage, @Value("${ocr.datapath}") String ocrDataPath) {
        this.storageService = storageService;
        this.ocrLanguage = ocrLanguage;
        this.ocrDataPath = ocrDataPath;
    }

    @Override
    public List<String> supportedFileFormats() {
        return SUPPORTED_FILE_FORMATS;
    }

    @Override
    public List<Page> doOcr(File inputFile) throws OcrException {
        List<Page> pages = new ArrayList<>();
        try {
            BufferedImage originalPageImage = ImageIO.read(inputFile);
            String pageText = null;
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
            }
            log.debug("ocr finished");

            File previewImageFile = File.createTempFile("paper{s}pace-preview", ".png");
            log.debug("writing preview image for page to [{}]", previewImageFile);
            if (writeImageFile(originalPageImage, "png", previewImageFile)) {
                String mimeType = FileTypes.loadMimeType(previewImageFile);
                Binary imageBinary = this.storageService.store(UploadType.IMAGE, previewImageFile, "ps_preview_image_" + UUID.randomUUID() + ".png", mimeType);
                pages.add(new Page(UUID.randomUUID(), 0, pageText, imageBinary));
            }
        } catch (IOException e) {
            throw new OcrException(e);
        }

        return pages;
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
        writer.write(metadata, new IIOImage(Thumbnails.of(image).height(1024).asBufferedImage(), null, metadata), writeParam);
        stream.close();
        return true;
    }

}
