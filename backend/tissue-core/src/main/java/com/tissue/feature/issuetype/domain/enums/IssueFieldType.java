package com.tissue.feature.issuetype.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "Data type for a custom issue field. "
                + "TEXT is multi-line free text (rendered as Markdown); SHORT_TEXT is single-line and length-limited. "
                + "SELECT_OPTION and CHECKLIST support predefined options; other types accept direct values.")
public enum IssueFieldType {
    TEXT,
    SHORT_TEXT,
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
