package com.tissue.shared.meta;

public enum Evaluation {
    APPROVED,

    /** Behavior is correct, but might have minor issues.*/
    ACCEPTABLE,

    NEEDS_REFACTOR,

    PERFORMANCE_PROBLEM,

    SECURITY_PROBLEM,

    NOT_REVIEWED,
}
