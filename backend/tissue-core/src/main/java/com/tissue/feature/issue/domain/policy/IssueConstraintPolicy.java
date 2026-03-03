package com.tissue.feature.issue.domain.policy;

public interface IssueConstraintPolicy {
    int TITLE_MIN_LENGTH = 2;
    int TITLE_MAX_LENGTH = 100;

    int CONTENT_MAX_LENGTH = 65535;

    int SUMMARY_MAX_LENGTH = 2000;
}
