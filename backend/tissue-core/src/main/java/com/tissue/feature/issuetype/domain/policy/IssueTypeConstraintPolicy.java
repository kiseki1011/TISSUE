package com.tissue.feature.issuetype.domain.policy;

public interface IssueTypeConstraintPolicy {
    int NAME_MIN_LENGTH = 2;
    int NAME_MAX_LENGTH = 50;

    int DESCRIPTION_MAX_LENGTH = 255;
}
