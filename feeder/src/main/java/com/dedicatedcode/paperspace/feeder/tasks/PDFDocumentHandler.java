package com.dedicatedcode.paperspace.feeder.tasks;

import com.dedicatedcode.paperspace.feeder.FileStatus;
import com.dedicatedcode.paperspace.feeder.InputType;
import com.dedicatedcode.paperspace.feeder.configuration.AppConfiguration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.recognition.software.jdeskew.ImageDeskew;
import net.sf.jmimemagic.*;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.ImageHelper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PDFDocumentHandler extends FileSizeCheckingTask {
    private static final Logger log = LoggerFactory.getLogger(PDFDocumentHandler.class);
    private static final Gson GSON = new GsonBuilder().create();
    private static final double MINIMUM_DESKEW_THRESHOLD = 0.05d;

    public PDFDocumentHandler(File file, InputType inputType, AppConfiguration configuration, ApiAvailabilityService availabilityService) {
        super(file, inputType, configuration, availabilityService);
    }

    @Override
    protected FileStatus handle(File incoming) throws IOException {
        log.debug("uploading pdf [{}] to api", incoming);
        UUID mainBinary = uploadBinary(incoming);
        if (mainBinary == null) {
            return FileStatus.ERROR;
        }
        log.debug("uploaded pdf [{}] to api with id [{}]", incoming, mainBinary);

        IDResponse documentResponse = uploadDocument(incoming.getName(), mainBinary);

        PDFTextStripper textStripper = new PDFTextStripper();

        PDDocument document = PDDocument.load(incoming);
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        int pageCounter = 0;
        for (PDPage ignored : document.getPages()) {
            textStripper.setStartPage(pageCounter);
            BufferedImage originalPageImage = pdfRenderer.renderImageWithDPI(pageCounter, 300, ImageType.RGB);
            BufferedImage ocrImage = null;

            pageCounter += 1;
            textStripper.setEndPage(pageCounter);
            String pageText = textStripper.getText(document);
            if (!StringUtils.isEmpty(pageText) && !pageText.replace("\n", "").isEmpty()) {
                log.debug("page contains text, will skip ocr run");
            } else {
                log.debug("could not extract text of page, will submit to ocr process");
                ImageDeskew id = new ImageDeskew(originalPageImage);
                double imageSkewAngle = id.getSkewAngle();
                if ((imageSkewAngle > MINIMUM_DESKEW_THRESHOLD || imageSkewAngle < -(MINIMUM_DESKEW_THRESHOLD))) {
                    log.debug("image skew angle was [{}] which is over [{}] allowed, will deskew the image", imageSkewAngle, MINIMUM_DESKEW_THRESHOLD);
                    ocrImage = ImageHelper.rotateImage(originalPageImage, -imageSkewAngle);
                } else {
                    ocrImage = originalPageImage;
                }

                Tesseract tesseract = new Tesseract();
                try {
                    tesseract.setLanguage(this.configuration.getOcr().getLanguage());
                    tesseract.setDatapath(this.configuration.getOcr().getDatapath());
                    pageText = tesseract.doOCR(ocrImage);
                } catch (TesseractException e) {
                    log.warn("could not extract content of page for [{}], will pretend no text", incoming, e);
                    pageText = null;
                }
            }
            uploadPage(documentResponse, originalPageImage, pageText, pageCounter);
        }
        document.close();

        return FileStatus.PROCESSED;
    }

    private void uploadPage(IDResponse document, BufferedImage previewImage, String text, int pageNumber) {
        // write image to tmp file
        try {
            File previewImageFile = File.createTempFile("paper{s}pace-preview", ".png");
            if (ImageIOUtil.writeImage(previewImage, previewImageFile.getAbsolutePath(), 300)) {
                UUID uploadBinaryId = uploadBinary(previewImageFile);
                String pagesUrl = document.getLinks().pages;
                try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
                    HttpPost uploadDocumentRequest = new HttpPost(this.configuration.getApi().getHost() + pagesUrl);
                    HttpEntity entity = EntityBuilder.create()
                            .setContentType(ContentType.APPLICATION_JSON)
                            .setText(GSON.toJson(new PageUpload(text, pageNumber, uploadBinaryId))).build();

                    uploadDocumentRequest.setEntity(entity);
                    CloseableHttpResponse response = httpClient.execute(uploadDocumentRequest);
                    String responseBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == 201) {
                        IDResponse idResponse = GSON.fromJson(responseBody, IDResponse.class);
                        log.info("Uploaded page with id [{}]", idResponse.getId());
                    } else {
                        log.warn("sending page to api results in [{}] with status code[{}]", responseBody, statusCode);
                    }
                }
            }
        } catch (IOException e) {
            log.error("could not write preview image to tmp file.", e);
        }
    }

    private IDResponse uploadDocument(String title, UUID binaryId) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost uploadDocumentRequest = new HttpPost(this.configuration.getApi().getHost() + (inputType == InputType.DOCUMENT ? "/document" : "/task"));
            HttpEntity entity = EntityBuilder.create()
                    .setContentType(ContentType.APPLICATION_JSON)
                    .setText(GSON.toJson(new DocumentUpload(title, binaryId.toString()))).build();
            uploadDocumentRequest.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(uploadDocumentRequest);
            String responseBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            int uploadBinaryStatusCode = response.getStatusLine().getStatusCode();

            if (uploadBinaryStatusCode == 201) {
                IDResponse idResponse = GSON.fromJson(responseBody, IDResponse.class);
                log.info("Uploaded document with id [{}]", idResponse.getId());
                return idResponse;
            } else {
                log.warn("sending binary to api results in [{}] with status code[{}]", responseBody, uploadBinaryStatusCode);
                return null;
            }
        }
    }

    private UUID uploadBinary(File path) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost uploadBinaryRequest = new HttpPost(this.configuration.getApi().getHost() + "/binary");

            FileBody fileBody = new FileBody(path, ContentType.DEFAULT_BINARY, path.getName());
            String fileMimeType;
            try {
                MagicMatch magicMatch = Magic.getMagicMatch(path, true, false);
                fileMimeType = magicMatch.getMimeType();
            } catch (MagicParseException | MagicMatchNotFoundException | MagicException e) {
                log.warn("could not extract mimeType of [{}], will assume octet-stream", path);
                fileMimeType = "application/octet-stream";
            }
            StringBody mimeType = new StringBody(fileMimeType, ContentType.MULTIPART_FORM_DATA);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addPart("data", fileBody);
            builder.addPart("mimeType", mimeType);
            HttpEntity entity = builder.build();
            uploadBinaryRequest.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(uploadBinaryRequest);
            String responseBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            int uploadBinaryStatusCode = response.getStatusLine().getStatusCode();
            if (uploadBinaryStatusCode == 201) {
                IDResponse idResponse = GSON.fromJson(responseBody, IDResponse.class);
                log.info("Uploaded binary with id [{}]", idResponse.getId());
                return idResponse.getId();
            } else {
                log.warn("sending binary to api results in [{}] with status code[{}]", responseBody, uploadBinaryRequest);
                return null;
            }
        }
    }

    private static final class DocumentUpload {
        private final String title;
        private final String binaryId;

        private DocumentUpload(String title, String binaryId) {
            this.title = title;
            this.binaryId = binaryId;
        }

        public String getTitle() {
            return title;
        }

        public String getBinaryId() {
            return binaryId;
        }
    }

    private static final class IDResponse {
        private final UUID id;
        private final Links links;

        public IDResponse(UUID id, Links links) {
            this.id = id;
            this.links = links;
        }

        public UUID getId() {
            return id;
        }

        public Links getLinks() {
            return links;
        }
    }

    private static class Links {
        private final String self;
        private final String pages;

        private Links(String self, String pages) {
            this.self = self;
            this.pages = pages;
        }

        public String getSelf() {
            return self;
        }

        public String getPages() {
            return pages;
        }
    }

    private static class PageUpload {
        private final String text;
        private final int number;
        private final UUID binaryId;

        public PageUpload(String text, int number, UUID binaryId) {
            this.text = text;
            this.number = number;
            this.binaryId = binaryId;
        }

        public String getText() {
            return text;
        }

        public int getNumber() {
            return number;
        }

        public UUID getBinaryId() {
            return binaryId;
        }
    }
}
