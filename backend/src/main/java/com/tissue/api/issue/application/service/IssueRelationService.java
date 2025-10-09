package com.tissue.api.issue.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.finder.IssueFinder;
import com.tissue.api.issue.domain.enums.IssueRelationType;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.IssueRelation;
import com.tissue.api.issue.infrastructure.repository.IssueRelationRepository;
import com.tissue.api.issue.domain.service.CircularDependencyValidator;
import com.tissue.api.issue.presentation.dto.request.CreateIssueRelationRequest;
import com.tissue.api.issue.presentation.dto.response.IssueRelationResponse;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberFinder;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueRelationService {

	private final IssueFinder issueFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final IssueRelationRepository relationRepository;
	private final CircularDependencyValidator circularDependencyValidator;

	@Transactional
	public IssueRelationResponse createRelation(
		String workspaceCode,
		String sourceIssueKey,
		String targetIssueKey,
		Long memberId,
		CreateIssueRelationRequest request
	) {
		Issue sourceIssue = issueFinder.findIssue(sourceIssueKey, workspaceCode);
		Issue targetIssue = issueFinder.findIssue(targetIssueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceCode);

		// TODO: IssueAuthorizationService로 로직 분리, 호출
		// TODO: IssueAuthorizationInterceptor에서 IssueAuthorizationService를 호출하는 형태로 구현?
		// if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
		// 	sourceIssue.validateIsAssigneeOrAuthor(requester.getId());
		// }

		if (request.relationType() == IssueRelationType.BLOCKS) {
			circularDependencyValidator.validateNoCircularDependency(sourceIssue, targetIssue);
		}

		IssueRelation relation = IssueRelation.createRelation(sourceIssue, targetIssue, request.relationType());
		relationRepository.save(relation);

		return IssueRelationResponse.from(sourceIssue, targetIssue, relation);
	}

	@Transactional
	public void removeRelation(
		String workspaceCode,
		String sourceIssueKey,
		String targetIssueKey,
		Long memberId
	) {
		Issue sourceIssue = issueFinder.findIssue(sourceIssueKey, workspaceCode);
		Issue targetIssue = issueFinder.findIssue(targetIssueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceCode);

		// TODO: IssueAuthorizationService로 로직 분리, 호출
		// TODO: IssueAuthorizationInterceptor에서 IssueAuthorizationService를 호출하는 형태로 구현?
		// if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
		// 	sourceIssue.validateIsAssigneeOrAuthor(requester.getId());
		// }

		IssueRelation.removeRelation(sourceIssue, targetIssue);
	}
}
