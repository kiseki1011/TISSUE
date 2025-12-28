package com.tissue.workflow.domain.guard;

public record GuardParamMetaData(
        String key, GuardParamType type, Object defaultValue, boolean required) {
    public static GuardParamMetaData of(
            String key, GuardParamType type, Object defVal, boolean req) {
        return new GuardParamMetaData(key, type, defVal, req);
    }
}
