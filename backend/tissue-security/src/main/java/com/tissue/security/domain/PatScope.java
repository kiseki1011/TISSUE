package com.tissue.security.domain;

/**
 * Authorization scope of a {@link PersonalAccessToken}
 */
public enum PatScope {
    READ_ONLY,
    READ_WRITE;

    public boolean canWrite() {
        return this == READ_WRITE;
    }
}
