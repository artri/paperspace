package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/api/tags.json")
    public ResponseEntity<List<Tag>> getTags(@RequestParam(name = "search", required = false) String searchTerm) {
        CacheControl cacheControl = CacheControl.noStore()
                .noTransform()
                .mustRevalidate();
        return ResponseEntity.ok().cacheControl(cacheControl).body(this.tagService.getAll(searchTerm));
    }
}
