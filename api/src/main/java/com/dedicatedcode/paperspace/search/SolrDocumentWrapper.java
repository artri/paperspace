package com.dedicatedcode.paperspace.search;

import org.apache.solr.client.solrj.beans.Field;

public class SolrDocumentWrapper {
    @Field
    private String id;

    public SolrDocumentWrapper() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
