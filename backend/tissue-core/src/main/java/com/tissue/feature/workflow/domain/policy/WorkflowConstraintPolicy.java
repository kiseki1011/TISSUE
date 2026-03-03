package com.tissue.feature.workflow.domain.policy;

public interface WorkflowConstraintPolicy {
    int NAME_MIN_LENGTH = 2;
    int NAME_MAX_LENGTH = 32;

    int DESCRIPTION_MAX_LENGTH = 255;
}
