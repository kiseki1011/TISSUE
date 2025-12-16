package com.tissue.issue.domain.service.relation;

import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.enums.IssueRelationType;

public interface RelationCycleDetector {
	public void ensureNoCycle(Issue source, Issue target, IssueRelationType relationType);
}
