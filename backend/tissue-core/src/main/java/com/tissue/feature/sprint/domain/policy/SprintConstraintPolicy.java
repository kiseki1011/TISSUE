package com.tissue.feature.sprint.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SprintConstraintPolicy {
    public static final int TITLE_MIN_LENGTH = 2;
    public static final int TITLE_MAX_LENGTH = 50;

    public static final int GOAL_MAX_LENGTH = 255;
}
