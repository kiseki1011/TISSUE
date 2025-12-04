package com.tissue.api.issuetype.domain.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.issuetype.repository.IssueTypeQueryRepository;
import com.tissue.api.project.domain.Project;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueTypeValidator {

	private final IssueQueryRepository issueQueryRepo;
	private final IssueTypeQueryRepository issueTypeQueryRepo;

	public void ensureUniqueLabel(Project project, Label label) {
		boolean duplicated = issueTypeQueryRepo.existsByLabel_NormalizedAndProject(label.getNormalized(), project);
		if (duplicated) {
			// TODO: DuplicateIssueTypeException
			throw new RuntimeException("Issue type label already exists in this workspace.");
		}
	}

	public void ensureDeletable(IssueType type) {
		ensureNotSystemType(type);
		ensureNotInUse(type);
	}

	private void ensureNotSystemType(IssueType type) {
		if (type.isSystemType()) {
			// TODO: SystemTypeNotDeletableException -> extends BadRequestException
			throw new RuntimeException("Cannot delete system(default) issue types.");
		}
	}

	private void ensureNotInUse(IssueType type) {
		if (issueQueryRepo.existsByIssueType(type)) {
			// TODO: IssueTypeInUseException
			throw new RuntimeException("Cannot delete: issues exist for this issue type.");
		}
	}
}
