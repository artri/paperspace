package com.dedicatedcode.paperspace.web;

import java.util.List;

public class PageEditModel {
    private final PageResponse page;
    private final List<PageEditTransformation> transformations;

    public PageEditModel(PageResponse page, List<PageEditTransformation> transformations) {
        this.page = page;
        this.transformations = transformations;
    }

    public PageResponse getPage() {
        return page;
    }

    public List<PageEditTransformation> getTransformations() {
        return transformations;
    }
}
