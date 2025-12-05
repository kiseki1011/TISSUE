package com.tissue.api.issuetype.application.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.application.port.out.IssueFieldValueQueryRepository;
import com.tissue.api.issuetype.application.port.out.IssueFieldQueryRepository;
import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issuetype.domain.IssueType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFieldValidator {

	private final IssueFieldQueryRepository issueFieldRepo;
	private final IssueFieldValueQueryRepository fieldValueRepo;

	public void ensureUniqueLabel(IssueType type, Label label) {
		boolean duplicated = issueFieldRepo.existsByIssueTypeAndLabel_Normalized(type, label.getNormalized());
		if (duplicated) {
			// TODO: DuplicateIssueFieldException
			throw new RuntimeException("Label already exists for this issue type.");
		}
	}

	public void ensureDeletable(IssueField field) {
		ensureNotInUse(field);
	}

	private void ensureNotInUse(IssueField field) {
		boolean fieldInUse = fieldValueRepo.existsByField(field);
		if (fieldInUse) {
			// TODO: IssueFieldInUseException
			throw new RuntimeException("Field is in use.");
		}
	}
}
