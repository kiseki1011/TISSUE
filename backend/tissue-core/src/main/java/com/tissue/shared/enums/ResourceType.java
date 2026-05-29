package com.tissue.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourceType {
    ISSUE,
    ISSUE_COMMENT,
    SPRINT,
    PROJECT,
    PROJECT_MEMBER
}
