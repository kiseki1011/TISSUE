package com.tissue.feature.issuetype.domain.enums;

public enum IssueFieldType {
    TEXT,
    INTEGER,
    DECIMAL,
    TIMESTAMP,
    DATE,
    BOOLEAN,
    SELECT_OPTION,
    PERCENTAGE,
    CHECKLIST;

    public boolean canHaveOptions() {
        return this == SELECT_OPTION || this == CHECKLIST;
    }
}
