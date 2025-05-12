package com.tissue.api.issue.application.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.service.reader.IssueReader;
import com.tissue.api.issue.domain.model.enums.IssueStatus;
import com.tissue.api.issue.domain.event.IssueCreatedEvent;
import com.tissue.api.issue.domain.event.IssueParentAssignedEvent;
import com.tissue.api.issue.domain.event.IssueParentRemovedEvent;
import com.tissue.api.issue.domain.event.IssueStatusChangedEvent;
import com.tissue.api.issue.domain.event.IssueUpdatedEvent;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.infrastructure.repository.IssueRepository;
import com.tissue.api.issue.presentation.controller.dto.request.AddParentIssueRequest;
import com.tissue.api.issue.presentation.controller.dto.request.UpdateIssueStatusRequest;
import com.tissue.api.issue.presentation.controller.dto.request.create.CreateIssueRequest;
import com.tissue.api.issue.presentation.controller.dto.request.update.UpdateIssueRequest;
import com.tissue.api.issue.presentation.controller.dto.response.IssueResponse;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspace.application.service.command.WorkspaceReader;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;

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
	public IssueResponse createIssue(
		String workspaceCode,
		Long memberId,
		CreateIssueRequest request
	) {
		Workspace workspace = workspaceReader.findWorkspace(workspaceCode);

		Issue issue = request.toIssue(workspace);
		Issue savedIssue = issueRepository.save(issue);

		watchIssue(workspaceCode, savedIssue.getIssueKey(), memberId);

		eventPublisher.publishEvent(
			IssueCreatedEvent.createEvent(issue, memberId)
		);

		return IssueResponse.from(savedIssue);
	}

	@Transactional
	public IssueResponse updateIssue(
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

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse updateIssueStatus(
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

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse assignParentIssue(
		String workspaceCode,
		String issueKey,
		Long memberId,
		AddParentIssueRequest request
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

		return IssueResponse.from(childIssue);
	}

	@Transactional
	public IssueResponse removeParentIssue(
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

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse watchIssue(
		String workspaceCode,
		String issueKey,
		Long memberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		issue.addWatcher(workspaceMember);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse unwatchIssue(
		String workspaceCode,
		String issueKey,
		Long memberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		issue.removeWatcher(workspaceMember);

		return IssueResponse.from(issue);
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
