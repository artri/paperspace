package com.dedicatedcode.paperspace.search;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SolrVersionService {
    public enum Status {
        UP_TO_DATE,
        NEEDS_UPGRADE,
        TO_NEW
    }

    static final int SUPPORTED_SCHEMA_VERSION = 9;
    private final SolrClient solrClient;

    public SolrVersionService(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public Status needsReindexing() {
        SolrQuery query = new SolrQuery("*:*");
        query.addFacetField("_schema_version_");
        query.addField("_schema_version_");
        query.addFilterQuery("-_schema_version_:" + SUPPORTED_SCHEMA_VERSION);
        try {
            QueryResponse response = this.solrClient.query(query);
            if (response.getResults().isEmpty()) {
                return Status.UP_TO_DATE;
            } else {
                List<FacetField.Count> collect = response.getFacetFields().stream().filter(facetField -> facetField.getValueCount() != 0).map(facetField -> facetField.getValues().get(0)).collect(Collectors.toList());
                Set<Integer> knownVersions = collect.stream().map(count -> {
                    try {
                        return Integer.parseInt(count.getName());
                    } catch (NumberFormatException ex) {
                        return 0;
                    }
                }).collect(Collectors.toSet());
                if (knownVersions.stream().anyMatch(integer -> integer > SUPPORTED_SCHEMA_VERSION)) {
                    return Status.TO_NEW;
                } else {
                    return Status.NEEDS_UPGRADE;
                }
            }
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException("Unable to connect to solr", e);
        }
    }
}
