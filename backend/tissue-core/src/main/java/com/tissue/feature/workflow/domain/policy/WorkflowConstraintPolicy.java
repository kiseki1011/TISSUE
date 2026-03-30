package com.tissue.feature.workflow.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WorkflowConstraintPolicy {

    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 32;

    public static final int DESCRIPTION_MAX_LENGTH = 255;
}
