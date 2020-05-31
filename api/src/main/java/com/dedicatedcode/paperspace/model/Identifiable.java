package com.dedicatedcode.paperspace.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by daniel on 11.12.16.
 * (c) 2016 Daniel Wasilew <daniel@dedicatedcode.com>
 */
public abstract class Identifiable implements Serializable {
    private final UUID id;

    public Identifiable(UUID id) {
        this.id = id;
    }

    public final UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Identifiable)) {
            return false;
        }

        Identifiable that = (Identifiable) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
