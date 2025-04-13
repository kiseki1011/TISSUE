package com.tissue.api.issue.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.event.IssueCreatedEvent;
import com.tissue.api.issue.domain.event.IssueParentAssignedEvent;
import com.tissue.api.issue.domain.event.IssueParentRemovedEvent;
import com.tissue.api.issue.domain.event.IssueStatusChangedEvent;
import com.tissue.api.issue.domain.event.IssueUpdatedEvent;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.presentation.dto.request.AssignParentIssueRequest;
import com.tissue.api.issue.presentation.dto.request.UpdateIssueStatusRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateIssueRequest;
import com.tissue.api.issue.presentation.dto.request.update.UpdateIssueRequest;
import com.tissue.api.issue.presentation.dto.response.AddWatcherResponse;
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

		// TODO: author를 watcher로 추가하도록 서비스 호출(addWatcher)

		eventPublisher.publishEvent(
			IssueCreatedEvent.createEvent(issue, currentWorkspaceMemberId)
		);

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

		Integer oldStoryPoint = issue.getStoryPoint();

		request.updateNonNullFields(issue);

		eventPublisher.publishEvent(
			IssueUpdatedEvent.createEvent(issue, oldStoryPoint, requesterWorkspaceMemberId)
		);

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

		IssueStatus oldStatus = issue.getStatus();

		issue.updateStatus(request.status());

		eventPublisher.publishEvent(
			IssueStatusChangedEvent.createEvent(issue, oldStatus, requesterWorkspaceMemberId)
		);

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

		Issue oldParentIssue = childIssue.getParentIssue();

		childIssue.updateParentIssue(parentIssue);

		eventPublisher.publishEvent(
			IssueParentAssignedEvent.createEvent(childIssue, parentIssue, oldParentIssue, requesterWorkspaceMemberId)
		);

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

		Issue oldParentIssue = issue.getParentIssue();

		// sub-task의 부모 제거 방지용 로직
		issue.validateCanRemoveParent();
		issue.removeParentRelationship();

		eventPublisher.publishEvent(
			IssueParentRemovedEvent.createEvent(issue, oldParentIssue, requesterWorkspaceMemberId)
		);

		return RemoveParentIssueResponse.from(issue);
	}

	@Transactional
	public AddWatcherResponse addWatcher(
		String workspaceCode,
		String issueKey,
		Long currentWorkspaceMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(
			currentWorkspaceMemberId,
			workspaceCode
		);

		issue.addWatcher(workspaceMember);

		return AddWatcherResponse.from(workspaceMember, issue);
	}

	@Transactional
	public void removeWatcher(
		String workspaceCode,
		String issueKey,
		Long currentWorkspaceMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(
			currentWorkspaceMemberId,
			workspaceCode
		);

		issue.removeWatcher(workspaceMember);
	}

	// @Transactional
	// public void softDeleteIssue(
	// 	String workspaceCode,
	// 	String issueKey,
	// 	Long requesterWorkspaceMemberId
	// ) {
	// 	Issue issue = issueReader.findIssue(issueKey, workspaceCode);
	// 	WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterWorkspaceMemberId);
	//
	// 	IssueStatus oldStatus = issue.getStatus();
	//
	// 	issue.delete();
	//
	// 	eventPublisher.publishEvent(
	// 		IssueStatusChangedEvent.createEvent(issue, oldStatus, requesterWorkspaceMemberId)
	// 	);
	// }
}
