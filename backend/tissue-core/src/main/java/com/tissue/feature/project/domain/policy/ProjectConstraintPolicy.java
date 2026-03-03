package com.tissue.feature.project.domain.policy;

public interface ProjectConstraintPolicy {
    String KEY_REGEX = "^[A-Z]+[0-9]*$";
    int KEY_MIN_LENGTH = 2;
    int KEY_MAX_LENGTH = 10;

    int TITLE_MIN_LENGTH = 2;
    int TITLE_MAX_LENGTH = 60;

    int DESCRIPTION_MAX_LENGTH = 255;
}
