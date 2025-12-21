package com.tissue.issuetype.application.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.common.vo.Label;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issuetype.application.port.out.IssueTypeQueryRepository;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.issuetype.domain.exception.IssueTypeExceptions;
import com.tissue.project.domain.Project;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueTypeValidator {

	private final IssueQueryRepository issueQueryRepo;
	private final IssueTypeQueryRepository issueTypeQueryRepo;

	public void ensureUniqueLabel(Project project, Label label) {
		boolean duplicated = issueTypeQueryRepo.existsByLabel_NormalizedAndProject(label.getNormalized(), project);
		if (duplicated) {
			throw IssueTypeExceptions.duplicateTypeName(label, project);
		}
	}

	// TODO: should i just allow deletion of a IssueType even if its a system provided type?
	public void ensureDeletable(IssueType type) {
		// ensureNotSystemType(type);
		ensureNotInUse(type);
	}

	private void ensureNotSystemType(IssueType issueType) {
		if (issueType.isSystemType()) {
			throw IssueTypeExceptions.systemTypeNotDeletable(issueType);
		}
	}

	private void ensureNotInUse(IssueType issueType) {
		if (issueQueryRepo.existsByIssueType(issueType)) {
			throw IssueTypeExceptions.typeInUse(issueType);
		}
	}
}
