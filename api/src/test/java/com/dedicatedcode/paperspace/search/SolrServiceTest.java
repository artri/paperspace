package com.dedicatedcode.paperspace.search;

import com.dedicatedcode.paperspace.DocumentService;
import com.dedicatedcode.paperspace.SearchResponse;
import com.dedicatedcode.paperspace.TagService;
import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.Document;
import com.dedicatedcode.paperspace.model.OCRState;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SolrServiceTest {

    private SolrService solrService;
    private SolrClient solrClient;
    private DocumentService documentService;
    private TagService tagService;

    @BeforeEach
    void setUp() {
        this.solrClient = mock(SolrClient.class);
        this.documentService = mock(DocumentService.class);
        this.tagService = mock(TagService.class);
        this.solrService = new SolrService(solrClient, documentService, tagService, new SolrQueryBuilder());
    }
    /*
     Map<String, Object> pagination = new HashMap<>();
            if (page > 0) {
                pagination.put("previous", createSearchLink(queryString, tagIds, page - 1));
            }
            if (page < totalPages - 1) {
                pagination.put("next", createSearchLink(queryString, tagIds, page + 1));
            }
            List<String> pageLinks = new ArrayList<>();
            for (int i = 0; i < totalPages; i++) {
                pageLinks.add(createSearchLink(queryString, tagIds, i));
            }
            pagination.put("pages", pageLinks);
            pagination.put("page", page);
            pagination.put("startIndex", page * maxResults + 1);
            pagination.put("endIndex", page * maxResults + documents.size());
            pagination.put("results", numFound);
            pagination.put("totalPages", totalPages);
     */

    @Test
    void shouldContainCorrectPagination() throws SolrServerException, IOException {
        QueryResponse response = mock(QueryResponse.class);
        SolrDocumentList solrDocuments = mock(SolrDocumentList.class);
        when(response.getResults()).thenReturn(solrDocuments);

        when(response.getBeans(SolrDocumentWrapper.class)).thenReturn(IntStream.range(0, 10).mapToObj(value -> {
            SolrDocumentWrapper solrDocumentWrapper = new SolrDocumentWrapper();
            solrDocumentWrapper.setId(UUID.randomUUID().toString());
            return solrDocumentWrapper;
        }).collect(Collectors.toList()));
        when(solrDocuments.getNumFound()).thenReturn(55L);

        when(this.solrClient.query(any(SolrQuery.class))).thenReturn(response);
        when(this.documentService.getDocument(any(UUID.class)))
                .thenAnswer(invocation -> new Document(UUID.randomUUID(), LocalDateTime.now(), "Tets Document", "", new Binary(UUID.randomUUID(), LocalDateTime.now(), "", "", "", -1, OCRState.PROCESSED), Collections.emptyList(), Collections.emptyList()));

        SearchResponse recent = this.solrService.recent(0, 10);
        Map<String, Object> pagination = recent.getPagination();
        assertNull(pagination.get("previous"));
        assertEquals("/api/search.json?page=1", pagination.get("next"));
        assertEquals(6, ((List) pagination.get("pages")).size());
        assertEquals(0, pagination.get("page"));
        assertEquals(1, pagination.get("startIndex"));
        assertEquals(10, pagination.get("endIndex"));
        assertEquals(55L, pagination.get("results"));
        assertEquals(6L, pagination.get("totalPages"));

        recent = this.solrService.recent(3, 10);
        pagination = recent.getPagination();
        assertEquals("/api/search.json?page=4", pagination.get("next"));
        assertEquals("/api/search.json?page=2", pagination.get("previous"));
        assertEquals(6, ((List) pagination.get("pages")).size());
        assertEquals(3, pagination.get("page"));
        assertEquals(31, pagination.get("startIndex"));
        assertEquals(40, pagination.get("endIndex"));
        assertEquals(55L, pagination.get("results"));
        assertEquals(6L, pagination.get("totalPages"));

        when(response.getBeans(SolrDocumentWrapper.class)).thenReturn(IntStream.range(0, 5).mapToObj(value -> {
            SolrDocumentWrapper solrDocumentWrapper = new SolrDocumentWrapper();
            solrDocumentWrapper.setId(UUID.randomUUID().toString());
            return solrDocumentWrapper;
        }).collect(Collectors.toList()));

        recent = this.solrService.recent(5, 10);
        pagination = recent.getPagination();
        assertNull(pagination.get("next"));
        assertEquals("/api/search.json?page=4", pagination.get("previous"));
        assertEquals(6, ((List) pagination.get("pages")).size());
        assertEquals(5, pagination.get("page"));
        assertEquals(51, pagination.get("startIndex"));
        assertEquals(55, pagination.get("endIndex"));
        assertEquals(55L, pagination.get("results"));
        assertEquals(6L, pagination.get("totalPages"));
    }
}