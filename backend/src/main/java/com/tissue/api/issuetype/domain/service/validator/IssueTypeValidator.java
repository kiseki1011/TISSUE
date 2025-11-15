package com.tissue.api.issuetype.domain.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.domain.port.out.IssueQueryRepository;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.issuetype.repository.IssueTypeQueryRepository;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueTypeValidator {

	private final IssueQueryRepository issueQueryRepo;
	private final IssueTypeQueryRepository issueTypeQueryRepo;

	public void ensureUniqueLabel(Workspace workspace, Label label) {
		boolean duplicated = issueTypeQueryRepo.existsByLabel_NormalizedAndWorkspace(label.getNormalized(), workspace);
		if (duplicated) {
			// TODO: DuplicateIssueTypeLabelException vs DuplicateIssueTypeException
			//  vs DuplicateLabelException(위치는 common.exception.domain) 공용으로 두기
			throw new RuntimeException("Issue type label already exists in this workspace.");
		}
	}

	/**
	 * Ensures the IssueType can be deleted.
	 * Throws exception if issue type is a system type or an issue for the issue type exists.
	 */
	public void ensureDeletable(IssueType type) {
		ensureNotSystemType(type);
		ensureNotInUse(type);
	}

	public void ensureNotSystemType(IssueType type) {
		if (type.isSystemType()) {
			// TODO: SystemProvidedNotDeletableException -> extends BadRequestException vs ForbiddenException
			//  이름을 SystemTypeNotDeletableException 사용도 고려. 어느 이름이 좋을지 모르겠음.
			throw new RuntimeException("Cannot delete system(default) issue types.");
		}
	}

	public void ensureNotInUse(IssueType type) {
		if (issueQueryRepo.existsByIssueType(type)) {
			// TODO: IssueTypeNotDeletableException vs IssueTypeCurrentlyUsedException vs IssueTypeInUseNotDeletableException
			//  이름을 어떻게 정하는게 좋을지 모르겠음. 상황을 설명? or 원인을 설명?
			throw new RuntimeException("Cannot delete: issues exist for this issue type.");
		}
	}
}
