package com.dedicatedcode.paperspace.search;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SolrHealthIndicator implements HealthIndicator {


    private final SolrClient solrClient;

    public SolrHealthIndicator(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    @Override
    public Health health() {
        try {
            SolrPingResponse ping = this.solrClient.ping();
            if (ping.getStatus() == 0) {
                return Health.up().build();
            } else {
                return Health.down().withDetail("Status Code", ping.getStatus()).build();
            }
        } catch (SolrServerException | IOException e) {
           return Health.down(e).build();
        }
    }
}
