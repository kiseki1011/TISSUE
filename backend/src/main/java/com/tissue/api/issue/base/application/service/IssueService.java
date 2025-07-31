package com.tissue.api.issue.base.application.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.base.application.dto.CreateIssueCommand;
import com.tissue.api.issue.base.application.dto.UpdateIssueCommand;
import com.tissue.api.issue.base.application.finder.IssueFinder;
import com.tissue.api.issue.base.application.finder.IssueTypeFinder;
import com.tissue.api.issue.base.domain.event.IssueParentAssignedEvent;
import com.tissue.api.issue.base.domain.event.IssueParentRemovedEvent;
import com.tissue.api.issue.base.domain.model.Issue;
import com.tissue.api.issue.base.domain.model.IssueFieldValue;
import com.tissue.api.issue.base.domain.model.IssueTypeDefinition;
import com.tissue.api.issue.base.domain.service.IssueFieldSchemaValidator;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldValueRepository;
import com.tissue.api.issue.base.infrastructure.repository.IssueRepository;
import com.tissue.api.issue.base.presentation.dto.request.AddParentIssueRequest;
import com.tissue.api.issue.base.presentation.dto.response.IssueResponse;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberFinder;
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
public class IssueService {

	private final IssueFinder issueFinder;
	private final IssueTypeFinder issueTypeFinder;
	private final WorkspaceFinder workspaceFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final IssueFieldSchemaValidator fieldSchemaValidator;

	private final IssueRepository issueRepository;
	private final IssueFieldValueRepository fieldValueRepository;

	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public IssueResponse createIssue(CreateIssueCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceCode());
		IssueTypeDefinition issueType = issueTypeFinder.findIssueType(cmd.workspaceCode(), cmd.issueTypeKey());

		Issue issue = issueRepository.save(Issue.builder()
			.workspace(workspace)
			.issueType(issueType)
			.title(cmd.title())
			.content(cmd.content())
			.summary(cmd.summary())
			.priority(cmd.priority())
			.dueAt(cmd.dueAt())
			.build());

		List<IssueFieldValue> values = fieldSchemaValidator.validateAndExtract(cmd.customFields(), issue);
		fieldValueRepository.saveAll(values);

		// TODO: Should I get the memberId from the controller, or use the audit info like now?
		WorkspaceMember creator = workspaceMemberFinder.findWorkspaceMember(issue.getCreatedBy(), workspace.getCode());
		issue.addWatcher(creator);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse updateIssue(UpdateIssueCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceCode());

		if (cmd.title() != null) {
			issue.updateTitle(cmd.title());
		}
		if (cmd.content() != null) {
			issue.updateContent(cmd.content());
		}
		if (cmd.summary() != null) {
			issue.updateSummary(cmd.summary());
		}
		if (cmd.priority() != null) {
			issue.updatePriority(cmd.priority());
		}
		if (cmd.dueAt() != null) {
			issue.updateDueAt(cmd.dueAt());
		}

		if (cmd.customFields() != null && !cmd.customFields().isEmpty()) {
			List<IssueFieldValue> updates = fieldSchemaValidator.validateAndApplyPartialUpdate(
				cmd.customFields(), issue
			);
			fieldValueRepository.saveAll(updates);
		}

		return IssueResponse.from(issue);
	}

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
		Issue childIssue = issueFinder.findIssue(issueKey, workspaceCode);
		Issue parentIssue = issueFinder.findIssue(request.parentIssueKey(), workspaceCode);
		WorkspaceMember requester = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceCode);

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
		Issue issue = issueFinder.findIssue(issueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceCode);

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
		Issue issue = issueFinder.findIssue(issueKey, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceCode);

		issue.addWatcher(workspaceMember);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse unwatchIssue(
		String workspaceCode,
		String issueKey,
		Long memberId
	) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceCode);

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
