package com.tissue.feature.workflow.domain.guard;

public enum GuardType {
    BLOCKING_ISSUE_RESOLVE_REQUIRED,
    APPROVAL_REQUIRED,
    ASSIGNEE_REQUIRED,
    CHILD_ISSUES_RESOLVE_REQUIRED,
    LINKED_BRANCH_REQUIRED;
}
