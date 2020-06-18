package com.dedicatedcode.paperspace.feeder.tasks;

import com.dedicatedcode.paperspace.feeder.FileStatus;
import com.dedicatedcode.paperspace.feeder.InputType;
import com.dedicatedcode.paperspace.feeder.configuration.AppConfiguration;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.Thread.sleep;

public abstract class FileSizeCheckingTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(FileSizeCheckingTask.class);
    private final File file;
    final InputType inputType;
    final AppConfiguration configuration;

    FileSizeCheckingTask(File file, InputType inputType, AppConfiguration configuration) {
        this.file = file;
        this.inputType = inputType;
        this.configuration = configuration;
    }

    @Override
    public final void run() {

        try {
            while (!checkAPIAvailability()) {
                log.warn("api is not ready to accept uploads. Will wait 30seconds");
                sleep(30000);
            }
            log.debug("api is available, will proceed");
            FileStatus fileStatus = handle(file);
            log.info("File [{}] handling results in [{}]", file, fileStatus);
            boolean shouldMoveProcessedFile = fileStatus != FileStatus.PROCESSED || this.configuration.getConfigurationBy(inputType).isMoveToProcessed();
            if (shouldMoveProcessedFile) {
                Path targetPath = getTargetFolder(inputType, fileStatus).resolve(file.getName());
                log.info("file [{}] will be moved to [{}]", file, targetPath);
                Files.move(file.toPath(), targetPath);
            } else {
                log.info("file [{}] will be deleted since it is processed and should not be moved to archive folder", file);
                Files.deleteIfExists(file.toPath());
            }
        } catch (Exception e) {
            log.error("error occurred while handling file [{}]", file, e);
            try {
                Path targetPath = getTargetFolder(inputType, FileStatus.ERROR).resolve(file.getName());
                log.info("file [{}] will be moved to [{}]", file, targetPath);
                Files.move(file.toPath(), targetPath);
            } catch (IOException ioException) {
                log.info("could not move [{}] to error folder", file, ioException);
            }
        }
    }

    private boolean checkAPIAvailability() {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet statusRequest = new HttpGet(this.configuration.getApi().getHost() + "/status");
            CloseableHttpResponse response = httpClient.execute(statusRequest);
            if (response.getStatusLine().getStatusCode() != 200) {
                log.info("checking status of api returned status code [{}] with reason[{}]", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                return false;
            } else {
                //check solr state
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(response.getEntity().getContent());
                String dataStatus = node.get("data").getTextValue();
                switch (dataStatus) {
                    case "UP_TO_DATE":
                        return true;
                    case "NEEDS_UPGRADE":
                        log.warn("API needs reindexing of data. Please open the ui and proceed with updating the data.");
                        return false;
                    case "TO_NEW":
                        log.warn("API version is to old. Please upgrade the api.");
                        return false;
                    default:
                        log.warn("Unable to handle data status [{}] coming from API. Please update feeder.", dataStatus);
                        return false;
                }
            }
        } catch (IOException e) {
            log.warn("API is not reachable. Error was:", e);
            return false;
        }
    }

    private Path getTargetFolder(InputType inputType, FileStatus fileStatus) {
        switch (fileStatus) {
            case PROCESSED:
                return this.configuration.getConfigurationBy(inputType).getProcessed().toPath();
            case ERROR:
                return this.configuration.getConfigurationBy(inputType).getError().toPath();
            case IGNORED:
                return this.configuration.getConfigurationBy(inputType).getIgnored().toPath();
            default:
                throw new RuntimeException("unhandled file status [" + fileStatus + "]");
        }
    }

    protected abstract FileStatus handle(File path) throws IOException;
}
