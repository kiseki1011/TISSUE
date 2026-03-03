package com.tissue.feature.workspace.domain.policy;

public interface WorkspaceConstraintPolicy {
    String KEY_REGEX = "^[a-zA-Z][a-zA-Z0-9-]*[a-zA-Z0-9]$";
    int KEY_MIN_LENGTH = 3;
    int KEY_MAX_LENGTH = 22;

    int NAME_MIN_LENGTH = 2;
    int NAME_MAX_LENGTH = 50;

    int DESCRIPTION_MAX_LENGTH = 255;
}
