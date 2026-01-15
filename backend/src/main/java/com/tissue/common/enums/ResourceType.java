package com.tissue.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourceType {
    ISSUE,
    ISSUE_COMMENT,
    SPRINT,
    WORKSPACE,
    WORKSPACE_MEMBER,
    PROJECT,
    PROJECT_MEMBER
}
