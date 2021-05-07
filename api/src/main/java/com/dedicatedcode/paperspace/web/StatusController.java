package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.feeder.MergingFileEventHandler;
import com.dedicatedcode.paperspace.search.SolrVersionService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    private final SolrVersionService solrVersionService;
    private final MergingFileEventHandler fileEventHandler;

    public StatusController(SolrVersionService solrVersionService, MergingFileEventHandler fileEventHandler) {
        this.solrVersionService = solrVersionService;
        this.fileEventHandler = fileEventHandler;
    }

    @RequestMapping("/api/status.json")
    @ResponseBody
    public Status loadStatus() {
        return new Status(this.solrVersionService.needsReindexing(), fileEventHandler.getPendingChanges());
    }


    private static class Status {
        private final String data;
        private final int pendingChanges;

        private Status(SolrVersionService.Status data, int pendingChanges) {
            this.data = data.name();
            this.pendingChanges = pendingChanges;
        }

        public String getData() {
            return data;
        }

        public int getPendingChanges() {
            return pendingChanges;
        }
    }
}
