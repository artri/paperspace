package com.dedicatedcode.paperspace.search;

import com.dedicatedcode.paperspace.model.Document;
import com.dedicatedcode.paperspace.model.TaskDocument;
import org.apache.solr.client.solrj.beans.Field;

import java.time.ZoneOffset;
import java.util.Date;

public class SolrDocumentWrapper {
    @Field
    private String id;
    @Field
    private String title;
    @Field
    private Date createdAt;
    @Field
    private String description;
    @Field
    private String content;
    @Field
    private String documentType;

    public SolrDocumentWrapper() {
    }

    SolrDocumentWrapper(Document document) {
        this.id = document.getId().toString();
        this.title = document.getTitle();
        this.createdAt = new Date(document.getCreatedAt().toInstant(ZoneOffset.UTC).getEpochSecond());
        this.description = document.getDescription();
        this.content = document.getContent();
        this.documentType = document instanceof TaskDocument ? "TASK" : "DOCUMENT";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
}
