package com.tissue.api.issue.application.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.service.reader.IssueReader;
import com.tissue.api.issue.domain.event.IssueParentAssignedEvent;
import com.tissue.api.issue.domain.event.IssueParentRemovedEvent;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.infrastructure.repository.IssueRepository;
import com.tissue.api.issue.presentation.controller.dto.request.AddParentIssueRequest;
import com.tissue.api.issue.presentation.controller.dto.response.IssueResponse;
import com.tissue.api.workspace.application.service.command.WorkspaceReader;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

/**
 * TODO
 *  - Needs authorization logic. For example, restrict so only assignees or the author can update.
 *  But also allow WorkspaceMembers with or higher than MANAGER role be able to update.
 *  - Should i make a authorization service or use Spring Security?
 *  - Use soft-delete for issue instead of hard delete
 *  - Make IssueAssociateService and move watchIssue, unWatchIssue methods to it
 *    - IssueAssociateService should have the use-cases for assignees, watchers, reviewers, etc...
 *  - Move updateIssueStatus method to WorkflowService
 *  <p>
 *  Needed use-case methods
 *  - Issue Create
 *  - Issue Update(meta-data update)
 *  - Issue Soft-Delete
 *  - Set Parent Issue Relation
 *  - Remove Parent Issue Relation
 */
@Service
@RequiredArgsConstructor
public class IssueCommandService {

	private final IssueReader issueReader;
	private final WorkspaceReader workspaceReader;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final IssueRepository issueRepository;

	private final ApplicationEventPublisher eventPublisher;

	// @Transactional
	// public IssueResponse createIssue(
	// 	String workspaceCode,
	// 	Long memberId,
	// 	CreateIssueRequest request
	// ) {
	// 	Workspace workspace = workspaceReader.findWorkspace(workspaceCode);
	//
	// 	Issue issue = request.toIssue(workspace);
	// 	Issue savedIssue = issueRepository.save(issue);
	//
	// 	watchIssue(workspaceCode, savedIssue.getIssueKey(), memberId);
	//
	// 	eventPublisher.publishEvent(
	// 		IssueCreatedEvent.createEvent(issue, memberId)
	// 	);
	//
	// 	return IssueResponse.from(savedIssue);
	// }

	// @Transactional
	// public IssueResponse updateIssue(
	// 	String workspaceCode,
	// 	String issueKey,
	// 	Long memberId,
	// 	UpdateIssueRequest request
	// ) {
	// 	Issue issue = issueReader.findIssue(issueKey, workspaceCode);
	// 	WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);
	//
	// 	issue.validateIssueTypeMatch(request.getType());
	//
	// 	Integer oldStoryPoint = issue.getStoryPoint();
	//
	// 	request.updateNonNullFields(issue);
	//
	// 	eventPublisher.publishEvent(
	// 		IssueUpdatedEvent.createEvent(issue, oldStoryPoint, memberId)
	// 	);
	//
	// 	return IssueResponse.from(issue);
	// }

	// @Transactional
	// public IssueResponse updateIssueStatus(
	// 	String workspaceCode,
	// 	String issueKey,
	// 	Long memberId,
	// 	UpdateIssueStatusRequest request
	// ) {
	// 	Issue issue = issueReader.findIssue(issueKey, workspaceCode);
	// 	WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);
	//
	// 	IssueStatus oldStatus = issue.getStatus();
	//
	// 	issue.updateStatus(request.status());
	//
	// 	eventPublisher.publishEvent(
	// 		IssueStatusChangedEvent.createEvent(issue, oldStatus, memberId)
	// 	);
	//
	// 	return IssueResponse.from(issue);
	// }

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
