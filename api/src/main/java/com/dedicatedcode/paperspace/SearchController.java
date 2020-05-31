package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.search.SolrService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {
    private final SolrService solrService;

    public SearchController(SolrService solrService) {
        this.solrService = solrService;
    }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam(name = "q", required = false) String query, @RequestParam(name = "page", required = false, defaultValue = "0") int page) {
        if (query == null) {
            return this.solrService.recent(page, 20);
        } else {
            return this.solrService.query(query, page, 20);
        }
    }
}
