package com.tissue.api.issuetype.domain.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.domain.port.out.IssueQueryRepository;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.issuetype.repository.IssueTypeQueryRepository;
import com.tissue.api.workspace.domain.Workspace;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueTypeValidator {

	private final IssueQueryRepository issueQueryRepo;
	private final IssueTypeQueryRepository issueTypeQueryRepo;

	public void ensureUniqueLabel(Workspace workspace, Label label) {
		boolean duplicated = issueTypeQueryRepo.existsByLabel_NormalizedAndWorkspace(label.getNormalized(), workspace);
		if (duplicated) {
			// TODO: DuplicateIssueTypeException
			throw new RuntimeException("Issue type label already exists in this workspace.");
		}
	}

	public void ensureDeletable(IssueType type) {
		ensureNotSystemType(type);
		ensureNotInUse(type);
	}

	public void ensureNotSystemType(IssueType type) {
		if (type.isSystemType()) {
			// TODO: SystemTypeNotDeletableException -> extends BadRequestException vs ForbiddenException
			throw new RuntimeException("Cannot delete system(default) issue types.");
		}
	}

	public void ensureNotInUse(IssueType type) {
		if (issueQueryRepo.existsByIssueType(type)) {
			// TODO: IssueTypeInUseException
			throw new RuntimeException("Cannot delete: issues exist for this issue type.");
		}
	}
}
