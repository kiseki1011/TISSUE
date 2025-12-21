package com.tissue.issuetype.application.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.common.vo.Label;
import com.tissue.issue.application.port.out.IssueFieldValueQueryRepository;
import com.tissue.issuetype.application.port.out.IssueFieldQueryRepository;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.issuetype.domain.exception.IssueTypeExceptions;

import lombok.RequiredArgsConstructor;

// TODO: should i integrate this into the IssueTypeValidator?
@Component
@RequiredArgsConstructor
public class IssueFieldValidator {

	private final IssueFieldQueryRepository issueFieldRepo;
	private final IssueFieldValueQueryRepository fieldValueRepo;

	public void ensureUniqueLabel(IssueType issueType, Label label) {
		boolean duplicated = issueFieldRepo.existsByIssueTypeAndLabel_Normalized(issueType, label.getNormalized());
		if (duplicated) {
			throw IssueTypeExceptions.duplicateFieldName(label, issueType);
		}
	}

	// TODO: should i just allow to delete the issue field whether its in use or not?
	public void ensureDeletable(IssueField issueField) {
		ensureNotInUse(issueField);
	}

	private void ensureNotInUse(IssueField issueField) {
		boolean fieldInUse = fieldValueRepo.existsByField(issueField);
		if (fieldInUse) {
			throw IssueTypeExceptions.fieldInUse(issueField);
		}
	}
}
