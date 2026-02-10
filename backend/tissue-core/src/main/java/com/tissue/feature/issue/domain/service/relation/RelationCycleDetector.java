package com.tissue.feature.issue.domain.service.relation;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssueRelationType;

public interface RelationCycleDetector {
    public void ensureNoCycle(Issue source, Issue target, IssueRelationType relationType);
}
