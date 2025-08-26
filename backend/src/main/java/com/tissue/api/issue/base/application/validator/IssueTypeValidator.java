package com.tissue.api.issue.base.application.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.base.domain.model.IssueType;
import com.tissue.api.issue.base.infrastructure.repository.IssueRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueTypeValidator {

	private final IssueRepository issueRepo;

	/**
	 * Ensures the IssueType can be hard deleted.
	 * Throws exception if issue type is a system type or an issue for the issue type exists.
	 */
	public void ensureDeletable(IssueType type) {
		if (type.isSystemType()) {
			throw new InvalidOperationException("Cannot delete system(default) issue types.");
		}
		if (issueRepo.existsByIssueType(type)) {
			throw new InvalidOperationException("Cannot delete: issues exist for this issue type.");
		}
	}
}
