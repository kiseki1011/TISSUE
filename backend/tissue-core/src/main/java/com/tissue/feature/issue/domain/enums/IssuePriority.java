package com.tissue.feature.issue.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IssuePriority {
    BLOCKER(1),
    MAJOR(2),
    NORMAL(3),
    MINOR(4),
    TRIVIAL(5);

    private final int level;

    public boolean isMoreCritical(IssuePriority priority) {
        return this.level < priority.level;
    }
}
