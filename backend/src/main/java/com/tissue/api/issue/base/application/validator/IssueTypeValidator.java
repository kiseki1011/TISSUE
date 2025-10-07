package com.tissue.api.issue.base.application.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.common.exception.type.ResourceConflictException;
import com.tissue.api.issue.base.domain.model.IssueType;
import com.tissue.api.issue.base.domain.model.vo.Label;
import com.tissue.api.issue.base.infrastructure.repository.IssueRepository;
import com.tissue.api.issue.base.infrastructure.repository.IssueTypeRepository;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueTypeValidator {

	private final IssueRepository issueRepo;
	private final IssueTypeRepository issueTypeRepo;

	public void ensureUniqueLabel(Workspace workspace, Label label) {
		boolean duplicated = issueTypeRepo.existsByWorkspaceAndLabel_Normalized(workspace, label.getNormalized());
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
			throw new InvalidOperationException("Cannot delete system(default) issue types.");
		}
	}

	public void ensureNotInUse(IssueType type) {
		if (issueRepo.existsByIssueType(type)) {
			throw new InvalidOperationException("Cannot delete: issues exist for this issue type.");
		}
	}
}
