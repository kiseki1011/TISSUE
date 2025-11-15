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
			// TODO: DuplicateIssueFieldLabelException vs DuplicateIssueFieldException
			//  vs DuplicateLabelException 공용으로 두기
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
			// TODO: IssueFieldNotDeletableException vs IssueFieldCurrentlyUsedException vs IssueFieldInUseNotDeletableException
			//  이름을 어떻게 정하는게 좋을지 모르겠음. 상황을 설명? or 원인을 설명?
			throw new RuntimeException("Field is in use.");
		}
	}
}
