package com.tissue.api.issue.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.service.reader.IssueReader;
import com.tissue.api.issue.domain.model.enums.IssueRelationType;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.IssueRelation;
import com.tissue.api.issue.domain.service.checker.CircularDependencyChecker;
import com.tissue.api.issue.infrastructure.repository.IssueRelationRepository;
import com.tissue.api.issue.presentation.controller.dto.request.CreateIssueRelationRequest;
import com.tissue.api.issue.presentation.controller.dto.response.IssueRelationResponse;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueRelationCommandService {

	private final IssueReader issueReader;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final IssueRelationRepository relationRepository;
	private final CircularDependencyChecker circularDependencyChecker;

	@Transactional
	public IssueRelationResponse createRelation(
		String workspaceCode,
		String sourceIssueKey,
		String targetIssueKey,
		Long memberId,
		CreateIssueRelationRequest request
	) {
		Issue sourceIssue = issueReader.findIssue(sourceIssueKey, workspaceCode);
		Issue targetIssue = issueReader.findIssue(targetIssueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		// TODO: IssueAuthorizationService로 로직 분리, 호출
		// TODO: IssueAuthorizationInterceptor에서 IssueAuthorizationService를 호출하는 형태로 구현?
		// if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
		// 	sourceIssue.validateIsAssigneeOrAuthor(requester.getId());
		// }

		if (request.relationType() == IssueRelationType.BLOCKS) {
			circularDependencyChecker.validateNoCircularDependency(sourceIssue, targetIssue);
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
		Issue sourceIssue = issueReader.findIssue(sourceIssueKey, workspaceCode);
		Issue targetIssue = issueReader.findIssue(targetIssueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		// TODO: IssueAuthorizationService로 로직 분리, 호출
		// TODO: IssueAuthorizationInterceptor에서 IssueAuthorizationService를 호출하는 형태로 구현?
		// if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
		// 	sourceIssue.validateIsAssigneeOrAuthor(requester.getId());
		// }

		IssueRelation.removeRelation(sourceIssue, targetIssue);
	}
}
