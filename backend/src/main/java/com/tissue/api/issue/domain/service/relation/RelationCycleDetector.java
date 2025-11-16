package com.tissue.api.issue.domain.service.relation;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueRelationType;

public interface RelationCycleDetector {
	public void ensureNoCycle(Issue source, Issue target, IssueRelationType relationType);
}
