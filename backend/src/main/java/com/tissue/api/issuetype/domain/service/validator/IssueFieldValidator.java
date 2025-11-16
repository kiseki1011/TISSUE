package com.tissue.api.issuetype.domain.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.domain.port.out.IssueFieldValueQueryRepository;
import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.issuetype.repository.IssueFieldQueryRepository;

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

	// TODO: 해당 IssueField를 사용한 value가 존재하면 삭제 불가임. 그냥 삭제 허용할까?
	private void ensureNotInUse(IssueField field) {
		boolean fieldInUse = fieldValueRepo.existsByField(field);
		if (fieldInUse) {
			// TODO: IssueFieldInUseException
			throw new RuntimeException("Field is in use.");
		}
	}
}
