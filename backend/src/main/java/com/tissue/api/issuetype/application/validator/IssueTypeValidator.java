package com.tissue.api.issuetype.application.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.BadRequestException;
import com.tissue.api.common.exception.type.ResourceConflictException;
import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.application.port.out.IssueQueryRepository;
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
		boolean duplicated = issueTypeQueryRepo.existsByWorkspaceAndLabel_Normalized(workspace, label.getNormalized());
		if (duplicated) {
			throw new ResourceConflictException("Issue type label already exists in this workspace.");
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
			throw new BadRequestException("Cannot delete system(default) issue types.");
		}
	}

	public void ensureNotInUse(IssueType type) {
		if (issueQueryRepo.existsByIssueType(type)) {
			throw new BadRequestException("Cannot delete: issues exist for this issue type.");
		}
	}
}
