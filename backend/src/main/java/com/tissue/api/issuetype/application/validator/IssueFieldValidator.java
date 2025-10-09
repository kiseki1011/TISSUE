package com.tissue.api.issuetype.application.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.common.exception.type.ResourceConflictException;
import com.tissue.api.issue.domain.model.vo.Label;
import com.tissue.api.issue.infrastructure.repository.IssueFieldValueRepository;
import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.issuetype.repository.IssueFieldRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFieldValidator {

	private final IssueFieldRepository issueFieldRepo;
	private final IssueFieldValueRepository fieldValueRepo;

	public void ensureUniqueLabel(IssueType type, Label label) {
		boolean duplicated = issueFieldRepo.existsByIssueTypeAndLabel_Normalized(type, label.getNormalized());
		if (duplicated) {
			throw new ResourceConflictException("Label already exists for this issue type.");
		}
	}

	public void ensureDeletable(IssueField field) {
		ensureNotInUse(field);
	}

	private void ensureNotInUse(IssueField field) {
		boolean fieldInUse = fieldValueRepo.existsByField(field);
		if (fieldInUse) {
			throw new InvalidOperationException("Field is in use.");
		}
	}
}
