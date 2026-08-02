package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;

public interface HierarchyCountRow {

    IssueHierarchy getHierarchy();

    long getCount();
}
