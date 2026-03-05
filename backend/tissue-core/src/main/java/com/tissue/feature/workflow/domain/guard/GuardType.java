package com.tissue.feature.workflow.domain.guard;

public enum GuardType {
    NOT_BLOCKED,
    REQUIRED_APPROVAL,
    ASSIGNEE_REQUIRED,
    CHILD_ISSUES_RESOLVED;
}
