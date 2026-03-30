package com.tissue.feature.workspace.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WorkspaceConstraintPolicy {

    public static final String KEY_REGEX = "^[a-zA-Z][a-zA-Z0-9-]*[a-zA-Z0-9]$";
    public static final int KEY_MIN_LENGTH = 3;
    public static final int KEY_MAX_LENGTH = 22;

    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 50;

    public static final int DESCRIPTION_MAX_LENGTH = 255;
}
