package com.tissue.feature.workspace.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WorkspaceMemberConstraintPolicy {
    public static final String DISPLAY_NAME_REGEX = "^[\\p{L}]+$";
    public static final int DISPLAY_NAME_MIN_LENGTH = 3;
    public static final int DISPLAY_NAME_MAX_LENGTH = 35;
}
