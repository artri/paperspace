package com.dedicatedcode.paperspace.model;

import com.dedicatedcode.paperspace.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TagRemovalListener implements DocumentListener {
    private static final Logger log = LoggerFactory.getLogger(TagRemovalListener.class);
    private final TagService tagService;

    public TagRemovalListener(TagService tagService) {
        this.tagService = tagService;
    }

    @Override
    public void changed(Document oldVersion, Document newVersion) {
        boolean tagsChanged = !oldVersion.getTags().equals(newVersion.getTags());
        if (tagsChanged) {
            log.debug("Tags have changed on document [{}] will check if we can remove some tags", newVersion.getId());
            tagService.getUnassignedTags().forEach(tag -> {
                log.info("Deleted Tag [{}({})] because it has no usages anymore.", tag.getId(), tag.getName());
                tagService.delete(tag);
            });
        }
    }

    @Override
    public void created(Document taskDocument) {

    }

    @Override
    public void deleted(Document document) {
        tagService.getUnassignedTags().forEach(tag -> {
            log.info("Deleted Tag [{}({})] because it has no usages anymore.", tag.getId(), tag.getName());
            tagService.delete(tag);
        });
    }
}
