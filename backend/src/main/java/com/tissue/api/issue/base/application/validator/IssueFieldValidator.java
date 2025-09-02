package com.tissue.api.issue.base.application.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.common.exception.type.ResourceConflictException;
import com.tissue.api.common.util.TextNormalizer;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.model.IssueType;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldRepository;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldValueRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFieldValidator {

	private final IssueFieldRepository issueFieldRepo;
	private final IssueFieldValueRepository fieldValueRepo;

	public void ensureUniqueLabel(IssueType type, String label) {
		label = TextNormalizer.normalizeText(label);
		boolean duplicated = issueFieldRepo.existsByIssueTypeAndLabel(type, label);
		if (duplicated) {
			throw new ResourceConflictException("Field label already exists for this issue type.");
		}
	}

	public void ensureUniqueLabel(IssueType type, String label, Long excludeId) {
		label = TextNormalizer.normalizeText(label);
		boolean duplicated = issueFieldRepo.existsByIssueTypeAndLabelAndIdNot(type, label, excludeId);
		if (duplicated) {
			throw new ResourceConflictException("Field label already exists for this issue type.");
		}
	}

	public void ensureDeletable(IssueField field) {
		ensureNotInUse(field);
	}

	public void ensureNotInUse(IssueField field) {
		boolean fieldInUse = fieldValueRepo.existsByField(field);
		if (fieldInUse) {
			throw new InvalidOperationException("Field is in use.");
		}
	}
}
