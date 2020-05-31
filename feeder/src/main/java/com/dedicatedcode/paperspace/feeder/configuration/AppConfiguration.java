package com.dedicatedcode.paperspace.feeder.configuration;

import com.dedicatedcode.paperspace.feeder.InputType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "feeder")
public class AppConfiguration {
    private DocumentConfiguration documents;
    private DocumentConfiguration tasks;
    private ApiConfiguration api;
    private OCRConfiguration ocr;

    public DocumentConfiguration getDocuments() {
        return documents;
    }

    public void setDocuments(DocumentConfiguration documents) {
        this.documents = documents;
    }

    public DocumentConfiguration getTasks() {
        return tasks;
    }

    public void setTasks(DocumentConfiguration tasks) {
        this.tasks = tasks;
    }

    public ApiConfiguration getApi() {
        return api;
    }

    public void setApi(ApiConfiguration api) {
        this.api = api;
    }

    public OCRConfiguration getOcr() {
        return ocr;
    }

    public void setOcr(OCRConfiguration ocr) {
        this.ocr = ocr;
    }

    public DocumentConfiguration getConfigurationBy(InputType inputType) {
        switch (inputType) {
            case DOCUMENT:
                return documents;
            case TASK:
                return tasks;
            default:
                throw new RuntimeException("Unhandled input type [" + inputType + "]");
        }
    }
}
