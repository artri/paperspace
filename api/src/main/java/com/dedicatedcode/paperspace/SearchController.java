package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.search.SolrService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class SearchController {
    private final SolrService solrService;
    private final TagService tagService;

    public SearchController(SolrService solrService, TagService tagService) {
        this.solrService = solrService;
        this.tagService = tagService;
    }

    @GetMapping("/api/search.json")
    public ResponseEntity<SearchResponse> search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "tags", required = false, defaultValue = "") List<UUID> tagIds) {

        List<UUID> cleanedList = tagIds.stream().filter(uuid -> tagService.get(uuid) != null).collect(Collectors.toList());
        if (cleanedList.equals(tagIds)) {
            SearchResponse response;
            if (query == null && cleanedList.isEmpty()) {
                response = this.solrService.recent(page, 20);
            } else {
                response = this.solrService.query(query, tagIds, page, 20);
            }
            CacheControl cacheControl = CacheControl.noStore()
                    .noTransform()
                    .mustRevalidate();
            return ResponseEntity.ok().cacheControl(cacheControl).body(response);
        } else {
            URI newUri = createRedirectUri(query, page, cleanedList);
            return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).location(newUri).build();
        }
    }

    private URI createRedirectUri(String query, int page, List<UUID> tagIds) {
        UriBuilder builder = new DefaultUriBuilderFactory().builder().path("/api/search.json");
        if (query != null) {
            builder = builder.queryParam("q", query);
        }
        if (page != 0) {
            builder = builder.queryParam("page", page);
        }
        if (!tagIds.isEmpty()) {
            builder = builder.queryParam("tags", tagIds);
        }
        return builder.build();
    }
}
