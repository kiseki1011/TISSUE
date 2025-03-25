package com.tissue.api.issue.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.event.IssueCreatedEvent;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.presentation.dto.request.AssignParentIssueRequest;
import com.tissue.api.issue.presentation.dto.request.UpdateIssueStatusRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateIssueRequest;
import com.tissue.api.issue.presentation.dto.request.update.UpdateIssueRequest;
import com.tissue.api.issue.presentation.dto.response.AssignParentIssueResponse;
import com.tissue.api.issue.presentation.dto.response.RemoveParentIssueResponse;
import com.tissue.api.issue.presentation.dto.response.UpdateIssueStatusResponse;
import com.tissue.api.issue.presentation.dto.response.create.CreateIssueResponse;
import com.tissue.api.issue.presentation.dto.response.update.UpdateIssueResponse;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.service.command.WorkspaceReader;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueCommandService {

	private final IssueReader issueReader;
	private final WorkspaceReader workspaceReader;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final IssueRepository issueRepository;

	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public CreateIssueResponse createIssue(
		String workspaceCode,
		Long currentWorkspaceMemberId,
		CreateIssueRequest request
	) {
		Workspace workspace = workspaceReader.findWorkspace(workspaceCode);

		Issue issue = request.toIssue(workspace);
		Issue savedIssue = issueRepository.save(issue);

		eventPublisher.publishEvent(new IssueCreatedEvent(
			issue.getId(),
			issue.getIssueKey(),
			workspaceCode,
			currentWorkspaceMemberId
		));

		return CreateIssueResponse.from(savedIssue);
	}

	@Transactional
	public UpdateIssueResponse updateIssue(
		String workspaceCode,
		String issueKey,
		Long requesterWorkspaceMemberId,
		UpdateIssueRequest request
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterWorkspaceMemberId);

		issue.validateIssueTypeMatch(request.getType());

		// Todo: AuthorizationService를 만들어서 권한 검사 로직 분리
		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		request.updateNonNullFields(issue);

		// 이슈 업데이트 이슈 발행 필요

		return UpdateIssueResponse.from(issue);
	}

	/**
	 * Todo
	 *  - 추후에 상태 전이와 관련된 규칙이나 워크플로우를 위한 엔진을 만들고(InternalWorkflowEngine -> WorkflowEngine 인터페이스 만들기)
	 *  - WorkflowService라는 도메인 서비스를 만들어서 WorkflowEngine 호출해서 사용
	 *  - IssueCommandService는 비즈니스 서비스이기 때문 도메인 서비스인 WorkflowService를 호출하도록 설계 예정
	 */
	@Transactional
	public UpdateIssueStatusResponse updateIssueStatus(
		String workspaceCode,
		String issueKey,
		Long requesterWorkspaceMemberId,
		UpdateIssueStatusRequest request
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		issue.updateStatus(request.status());

		// 이슈 상태 변경 이벤트 발행 필요

		return UpdateIssueStatusResponse.from(issue);
	}

	@Transactional
	public AssignParentIssueResponse assignParentIssue(
		String workspaceCode,
		String issueKey,
		Long requesterWorkspaceMemberId,
		AssignParentIssueRequest request
	) {
		Issue childIssue = issueReader.findIssue(issueKey, workspaceCode);
		Issue parentIssue = issueReader.findIssue(request.parentIssueKey(), workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			childIssue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		childIssue.updateParentIssue(parentIssue);

		// 부모 변경(등록) 이벤트 발행 필요

		return AssignParentIssueResponse.from(childIssue);
	}

	@Transactional
	public RemoveParentIssueResponse removeParentIssue(
		String workspaceCode,
		String issueKey,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		// sub-task의 부모 제거 방지용 로직
		issue.validateCanRemoveParent();
		issue.removeParentRelationship();

		// 부모 제거 이벤트 발행 필요

		return RemoveParentIssueResponse.from(issue);
	}
}
