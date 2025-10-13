package com.tissue.api.issue.domain.service;

import com.tissue.api.issue.domain.enums.IssueRelationType;
import com.tissue.api.issue.domain.model.Issue;

public interface CycleDetector {
	public void ensureNoCycle(Issue source, Issue target, IssueRelationType relationType);
}
