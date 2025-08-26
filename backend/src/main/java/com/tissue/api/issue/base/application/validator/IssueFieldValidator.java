package com.tissue.api.issue.base.application.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.ResourceConflictException;
import com.tissue.api.issue.base.domain.model.IssueType;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFieldValidator {

	private final IssueFieldRepository issueFieldRepo;

	/**
	 * Ensures the label is unique under an IssueType.
	 */
	public void ensureUniqueLabel(IssueType type, String label) {
		boolean duplicated = issueFieldRepo.existsByIssueTypeAndLabel(type, label);
		if (duplicated) {
			throw new ResourceConflictException("Field label already exists for this issue type.");
		}
	}

	/**
	 * Ensures the label is unique under an IssueType, excluding the IssueField of excludeId.
	 */
	public void ensureUniqueLabel(IssueType type, String label, Long excludeId) {
		boolean duplicated = issueFieldRepo.existsByIssueTypeAndLabelAndIdNot(type, label, excludeId);
		if (duplicated) {
			throw new ResourceConflictException("Field label already exists for this issue type.");
		}
	}
}
