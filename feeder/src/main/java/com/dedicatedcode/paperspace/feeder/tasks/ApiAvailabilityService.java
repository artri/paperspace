package com.dedicatedcode.paperspace.feeder.tasks;

import com.dedicatedcode.paperspace.feeder.configuration.ApiConfiguration;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ApiAvailabilityService {
    private static final Logger log = LoggerFactory.getLogger(ApiAvailabilityService.class);

    private final ApiConfiguration configuration;

    public ApiAvailabilityService(ApiConfiguration configuration) {
        this.configuration = configuration;
    }

    public boolean checkAPIAvailability() {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet statusRequest = new HttpGet(this.configuration.getHost() + "/status");
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
}
