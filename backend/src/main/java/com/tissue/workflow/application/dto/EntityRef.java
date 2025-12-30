package com.tissue.workflow.application.dto;

import org.jspecify.annotations.Nullable;

public record EntityRef(@Nullable Long id, String tempKey) {

    public EntityRef {
        if ((id == null) == (tempKey == null)) {
            throw new IllegalArgumentException("One of id or tempKey must be provided");
        }
    }

    public boolean isExisting() {
        return id != null;
    }
}
