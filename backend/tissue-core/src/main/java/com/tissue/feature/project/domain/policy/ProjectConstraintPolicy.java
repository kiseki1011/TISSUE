package com.tissue.feature.project.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProjectConstraintPolicy {

    public static final String KEY_REGEX = "^[A-Z]+[0-9]*$";
    public static final int KEY_MIN_LENGTH = 2;
    public static final int KEY_MAX_LENGTH = 10;

    public static final int TITLE_MIN_LENGTH = 2;
    public static final int TITLE_MAX_LENGTH = 60;

    public static final int DESCRIPTION_MAX_LENGTH = 255;
}
