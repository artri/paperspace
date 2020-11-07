package com.dedicatedcode.paperspace.model;

import java.util.UUID;

public class Tag extends Identifiable {

    private final String name;

    public Tag(UUID id, String name) {
        super(id);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Tag withName(String name) {
        return new Tag(getId(), name);
    }
}
