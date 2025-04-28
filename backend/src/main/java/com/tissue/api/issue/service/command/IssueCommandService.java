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
		Long memberId,
		CreateIssueRequest request
	) {
		Workspace workspace = workspaceReader.findWorkspace(workspaceCode);

		Issue issue = request.toIssue(workspace);
		Issue savedIssue = issueRepository.save(issue);

		addWatcher(workspaceCode, savedIssue.getIssueKey(), memberId);

		eventPublisher.publishEvent(
			IssueCreatedEvent.createEvent(issue, memberId)
		);

		return CreateIssueResponse.from(savedIssue);
	}

	@Transactional
	public UpdateIssueResponse updateIssue(
		String workspaceCode,
		String issueKey,
		Long memberId,
		UpdateIssueRequest request
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		issue.validateIssueTypeMatch(request.getType());

		// Todo: IssueAuthorizationService를 만들어서 권한 검사 로직 분리?
		// TODO: IssueAuthorizationInterceptor에서 IssueAuthorizationService를 호출하는 형태로 구현?
		// if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
		// 	issue.validateIsAssigneeOrAuthor(memberId);
		// }

		Integer oldStoryPoint = issue.getStoryPoint();

		request.updateNonNullFields(issue);

		eventPublisher.publishEvent(
			IssueUpdatedEvent.createEvent(issue, oldStoryPoint, memberId)
		);

		return UpdateIssueResponse.from(issue);
	}

	@Transactional
	public UpdateIssueStatusResponse updateIssueStatus(
		String workspaceCode,
		String issueKey,
		Long memberId,
		UpdateIssueStatusRequest request
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		// if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
		// 	issue.validateIsAssigneeOrAuthor(memberId);
		// }

		IssueStatus oldStatus = issue.getStatus();

		issue.updateStatus(request.status());

		eventPublisher.publishEvent(
			IssueStatusChangedEvent.createEvent(issue, oldStatus, memberId)
		);

		return UpdateIssueStatusResponse.from(issue);
	}

	@Transactional
	public AssignParentIssueResponse assignParentIssue(
		String workspaceCode,
		String issueKey,
		Long memberId,
		AssignParentIssueRequest request
	) {
		Issue childIssue = issueReader.findIssue(issueKey, workspaceCode);
		Issue parentIssue = issueReader.findIssue(request.parentIssueKey(), workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		// if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
		// 	childIssue.validateIsAssigneeOrAuthor(memberId);
		// }

		Issue oldParentIssue = childIssue.getParentIssue();

		childIssue.updateParentIssue(parentIssue);

		eventPublisher.publishEvent(
			IssueParentAssignedEvent.createEvent(childIssue, parentIssue, oldParentIssue, memberId)
		);

		return AssignParentIssueResponse.from(childIssue);
	}

	@Transactional
	public RemoveParentIssueResponse removeParentIssue(
		String workspaceCode,
		String issueKey,
		Long memberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		// if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
		// 	issue.validateIsAssigneeOrAuthor(memberId);
		// }

		Issue oldParentIssue = issue.getParentIssue();

		// sub-task의 부모 제거 방지용 로직
		issue.validateCanRemoveParent();
		issue.removeParentRelationship();

		eventPublisher.publishEvent(
			IssueParentRemovedEvent.createEvent(issue, oldParentIssue, memberId)
		);

		return RemoveParentIssueResponse.from(issue);
	}

	@Transactional
	public AddWatcherResponse addWatcher(
		String workspaceCode,
		String issueKey,
		Long memberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		issue.addWatcher(workspaceMember);

		return AddWatcherResponse.from(workspaceMember, issue);
	}

	@Transactional
	public void removeWatcher(
		String workspaceCode,
		String issueKey,
		Long memberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

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
