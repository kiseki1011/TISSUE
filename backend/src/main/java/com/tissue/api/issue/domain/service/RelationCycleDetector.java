package com.tissue.api.issue.domain.service;

import com.tissue.api.issue.domain.enums.IssueRelationType;
import com.tissue.api.issue.domain.Issue;

public interface RelationCycleDetector {
	public void ensureNoCycle(Issue source, Issue target, IssueRelationType relationType);
}
