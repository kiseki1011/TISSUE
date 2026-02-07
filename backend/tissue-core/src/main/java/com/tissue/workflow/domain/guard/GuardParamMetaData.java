package com.tissue.workflow.domain.guard;

import org.jspecify.annotations.Nullable;

public record GuardParamMetaData(
        String key, GuardParamType type, @Nullable Object defaultValue, boolean required) {

    public static GuardParamMetaData of(String key, GuardParamType type, @Nullable Object defVal, boolean req) {
        return new GuardParamMetaData(key, type, defVal, req);
    }
}
