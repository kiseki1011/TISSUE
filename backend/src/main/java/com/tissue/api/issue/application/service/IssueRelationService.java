package com.tissue.api.issue.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.AddIssueRelationCommand;
import com.tissue.api.issue.application.finder.IssueFinder;
import com.tissue.api.issue.domain.enums.IssueRelationType;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.IssueRelation;
import com.tissue.api.issue.domain.service.CircularDependencyValidator;
import com.tissue.api.issue.infrastructure.repository.IssueRelationRepository;
import com.tissue.api.issue.presentation.dto.response.IssueRelationResponse;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberFinder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueRelationService {

	private final IssueFinder issueFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final IssueRelationRepository relationRepository;
	private final CircularDependencyValidator circularDependencyValidator;

	@Transactional
	public IssueRelationResponse add(AddIssueRelationCommand cmd) {
		Issue sourceIssue = issueFinder.findIssue(cmd.sourceIssueKey(), cmd.workspaceKey());
		Issue targetIssue = issueFinder.findIssue(cmd.targetIssueKey(), cmd.workspaceKey());

		if (cmd.relationType() == IssueRelationType.BLOCKS) {
			circularDependencyValidator.validateNoCircularDependency(sourceIssue, targetIssue);
		}

		IssueRelation relation = IssueRelation.create(sourceIssue, targetIssue, cmd.relationType());
		relationRepository.save(relation);

		return IssueRelationResponse.from(sourceIssue, targetIssue, relation);
	}

	@Transactional
	public void remove(
		String workspaceKey,
		String sourceIssueKey,
		String targetIssueKey
	) {
		Issue sourceIssue = issueFinder.findIssue(sourceIssueKey, workspaceKey);
		Issue targetIssue = issueFinder.findIssue(targetIssueKey, workspaceKey);

		IssueRelation.removeRelation(sourceIssue, targetIssue);
	}
}
