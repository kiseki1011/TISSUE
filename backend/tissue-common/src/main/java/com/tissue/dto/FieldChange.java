package com.tissue.dto;

import org.jspecify.annotations.Nullable;

public record FieldChange(@Nullable Object from, @Nullable Object to) {

    public static FieldChange of(@Nullable Object from, @Nullable Object to) {
        return new FieldChange(from, to);
    }
}
