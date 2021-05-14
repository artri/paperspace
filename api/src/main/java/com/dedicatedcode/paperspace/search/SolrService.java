package com.dedicatedcode.paperspace.search;

import com.dedicatedcode.paperspace.DocumentService;
import com.dedicatedcode.paperspace.SearchFilter;
import com.dedicatedcode.paperspace.SearchResponse;
import com.dedicatedcode.paperspace.TagService;
import com.dedicatedcode.paperspace.model.Document;
import com.dedicatedcode.paperspace.model.Identifiable;
import com.dedicatedcode.paperspace.model.Tag;
import com.dedicatedcode.paperspace.model.TaskDocument;
import com.dedicatedcode.paperspace.web.DocumentResponse;
import com.dedicatedcode.paperspace.web.TaskDocumentResponse;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.dedicatedcode.paperspace.search.SolrVersionService.SUPPORTED_SCHEMA_VERSION;

@Service
public class SolrService {
    private static final Logger log = LoggerFactory.getLogger(SolrService.class);

    private final SolrClient solrClient;
    private final DocumentService documentService;
    private final TagService tagService;
    private final SolrQueryBuilder queryBuilder;

    @Autowired
    public SolrService(SolrClient solrClient, DocumentService documentService, TagService tagService, SolrQueryBuilder queryBuilder) {
        this.solrClient = solrClient;
        this.documentService = documentService;
        this.tagService = tagService;
        this.queryBuilder = queryBuilder;
    }

    public SearchResponse recent(int page, int maxResults) {
        return query(null, Collections.emptyList(), Collections.emptyList(), page, maxResults);
    }

    public SearchResponse query(String queryString, List<UUID> tagIds, List<SearchFilter> filters, int page, int maxResults) {
        SolrQuery query = new SolrQuery(this.queryBuilder.build(queryString));
        query.setSort("createdAt", SolrQuery.ORDER.desc);
        query.setRows(maxResults);
        if (queryString != null) {
            query.setHighlight(true);
            query.addHighlightField("title");
            query.addHighlightField("description");
            query.addHighlightField("content");
        }
        for (UUID tagId : tagIds) {
            query.addFilterQuery("tags:" + tagId);
        }
        for (SearchFilter filter : filters) {
            if (filter == SearchFilter.OPEN_TASKS) {
                query.addFilterQuery("documentType: TASK");
                query.addFilterQuery("taskState: OPEN");
            } else {
                throw new IllegalStateException("Unexpected value: " + filter);
            }
        }
        query.setStart(page * maxResults);
        query.addFacetField("tags");
        query.setFacetMinCount(1);
        try {
            QueryResponse queryResponse = this.solrClient.query(query);
            Map<String, Map<String, List<String>>> highlighting = queryResponse.getHighlighting();
            List<SolrDocumentWrapper> documentWrappers = queryResponse.getBeans(SolrDocumentWrapper.class);
            List<DocumentResponse> documents = documentWrappers
                    .stream()
                    .map(solrDocumentWrapper -> this.documentService.getDocument(UUID.fromString(solrDocumentWrapper.getId())))
                    .filter(Objects::nonNull)
                    .map(document -> {
                        Map<String, List<String>> highlights = highlighting.get(document.getId().toString());
                        List<String> highlightLines;
                        if (highlights == null || highlights.isEmpty()) {
                            if (queryString != null && !queryString.equals("")) {
                                highlightLines = Arrays.stream(document.getContent().split("\n"))
                                        .filter(line -> line.toLowerCase().contains(queryString.toLowerCase()))
                                        .limit(5)
                                        .map(line -> line.replaceAll(Pattern.quote(queryString), "<em>" + queryString + "</em>"))
                                        .collect(Collectors.toList());
                            } else {
                                highlightLines = Arrays.asList(document.getContent().split("\n"));
                            }
                        } else {
                            highlightLines = highlights.getOrDefault("title", new ArrayList<>());
                            highlightLines.addAll(highlights.getOrDefault("description", new ArrayList<>()));
                            highlightLines.addAll(highlights.getOrDefault("content", new ArrayList<>()));
                        }
                        String previewText = highlightLines.stream().flatMap(s -> Arrays.stream(s.split("\n"))).limit(5).collect(Collectors.joining("\n"));
                        return document instanceof TaskDocument ? new TaskDocumentResponse((TaskDocument) document, previewText) : new DocumentResponse(document, previewText);
                    })
                    .collect(Collectors.toList());
            long numFound = queryResponse.getResults().getNumFound();
            long totalPages = numFound <= maxResults ? 1 : (numFound / maxResults);
            totalPages += numFound > maxResults && numFound % maxResults > 0 ? 1 : 0;


            Map<String, Object> pagination = new HashMap<>();
            if (page > 0) {
                pagination.put("previous", createSearchLink(queryString, tagIds, filters, page - 1));
            }
            if (page < totalPages - 1) {
                pagination.put("next", createSearchLink(queryString, tagIds, filters, page + 1));
            }
            List<String> pageLinks = new ArrayList<>();
            for (int i = 0; i < totalPages; i++) {
                pageLinks.add(createSearchLink(queryString, tagIds, filters, i));
            }
            pagination.put("pages", pageLinks);
            pagination.put("page", page);
            pagination.put("startIndex", page * maxResults + 1);
            pagination.put("endIndex", page * maxResults + documents.size());
            pagination.put("results", numFound);
            pagination.put("totalPages", totalPages);

            List<SearchResponse.TagFacet> tags = new ArrayList<>();
            FacetField tagFacets = queryResponse.getFacetField("tags");
            if (tagFacets != null) {
                tagFacets.getValues().forEach(count -> {
                    if (count.getName().equals("")) {
                        tags.add(new SearchResponse.TagFacet(null, "All", count.getCount(), false));
                    } else {
                        UUID id = UUID.fromString(count.getName());
                        Tag tag = this.tagService.get(id);
                        if (tag != null) {
                            tags.add(new SearchResponse.TagFacet(tag.getId(), tag.getName(), count.getCount(), tagIds.contains(id)));
                        }
                    }
                });
            }
            List<SearchResponse.FilterFacet> filterFacets = filters.stream().map(filter -> new SearchResponse.FilterFacet(filter.name(), -1, true)).collect(Collectors.toList());

            return new SearchResponse(documents, pagination, tags, filterFacets);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException("unable to query solr", e);
        }
    }

    private String createSearchLink(String queryString, List<UUID> tagIds, List<SearchFilter> filters, int page) {
        String link = "/api/search.json?";
        if (!StringUtils.isEmpty(queryString)) {
            link += "q=" + queryString + "&";
        }
        if (page > 0) {
            link += "page=" + page + "&";
        }
        if (!tagIds.isEmpty()) {
            link += "tags=" + tagIds.stream().map(UUID::toString).collect(Collectors.joining(",")) + "&";
        }
        if (!filters.isEmpty()) {
            link += "filters=" + filters.stream().map(Enum::name).collect(Collectors.joining(",")) + "&";
        }
        return link.substring(0, link.length() - 1);
    }

    public void index(Document document) {
        try {
            SolrInputDocument solrInput = new SolrInputDocument();
            solrInput.addField("id", document.getId().toString());
            solrInput.addField("title", document.getTitle());
            solrInput.addField("createdAt", new Date(document.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()));
            solrInput.addField("description", document.getDescription());
            solrInput.addField("documentType", document instanceof TaskDocument ? "TASK" : "DOCUMENT");
            solrInput.addField("taskState", document instanceof TaskDocument ? ((TaskDocument) document).getState().name() : "");
            solrInput.addField("_schema_version_", SUPPORTED_SCHEMA_VERSION);
            solrInput.addField("content", document.getContent());
            solrInput.addField("tags", document.getTags().stream().map(Identifiable::getId).map(UUID::toString).collect(Collectors.toList()));

            this.solrClient.add(solrInput, 0);
            log.debug("indexed document [{}] into solr", document.getId());
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException("Unable to store document in search", e);
        }
    }

    public void reindex() {
        try {
            this.solrClient.deleteByQuery("*:*");
            List<Document> documents = this.documentService.getAll();
            log.info("start indexing of [{}]nr of documents", documents.size());
            documents.forEach(this::index);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException("Unable to reindex all documents", e);
        }
    }

    public void delete(Document document) {
        try {
            UpdateResponse response = this.solrClient.deleteByQuery("id:" + document.getId(), 0);
            if (response.getStatus() == 0) {
                log.info("deletion of document [{}] was successful", document.getId());
            } else {
                log.warn("could not delete document [{}] from solr. Please run reindex again.", document.getId());
            }
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException("Unable to reindex all documents", e);
        }
    }
}
