package com.tissue.feature.workspace.domain.policy;

public interface WorkspaceMemberConstraintPolicy {
    String DISPLAY_NAME_REGEX = "^[\\p{L}]+$";
    int DISPLAY_NAME_MIN_LENGTH = 3;
    int DISPLAY_NAME_MAX_LENGTH = 35;
}
