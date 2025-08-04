package com.tissue.api.issue.base.application.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.base.application.dto.AssignParentIssueCommand;
import com.tissue.api.issue.base.application.dto.CreateIssueCommand;
import com.tissue.api.issue.base.application.dto.RemoveParentIssueCommand;
import com.tissue.api.issue.base.application.dto.UpdateIssueCommand;
import com.tissue.api.issue.base.application.finder.IssueFinder;
import com.tissue.api.issue.base.application.finder.IssueTypeFinder;
import com.tissue.api.issue.base.domain.model.Issue;
import com.tissue.api.issue.base.domain.model.IssueFieldValue;
import com.tissue.api.issue.base.domain.model.IssueType;
import com.tissue.api.issue.base.domain.service.IssueFieldSchemaValidator;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldValueRepository;
import com.tissue.api.issue.base.infrastructure.repository.IssueRepository;
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
		IssueType issueType = issueTypeFinder.findIssueType(cmd.workspaceCode(), cmd.issueTypeKey());

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
		WorkspaceMember creator = workspaceMemberFinder.findWorkspaceMember(issue.getCreatedBy(), workspace.getKey());
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
			List<IssueFieldValue> updateValues = fieldSchemaValidator.validateAndApplyPartialUpdate(
				cmd.customFields(),
				issue
			);
			fieldValueRepository.saveAll(updateValues);
		}

		return IssueResponse.from(issue);
	}

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
	public IssueResponse assignParentIssue(AssignParentIssueCommand cmd) {
		Issue child = issueFinder.findIssue(cmd.childIssueKey(), cmd.workspaceCode());
		Issue parent = issueFinder.findIssue(cmd.parentIssueKey(), cmd.workspaceCode());

		child.assignParentIssue(parent);

		return IssueResponse.from(child);
	}

	@Transactional
	public IssueResponse removeParentIssue(RemoveParentIssueCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceCode());

		// TODO: Need to implement a way to turn on/off requiring a parent.
		//  To put it easy, I need a way to allow or disallow stand-alone creation of Issues.
		//  For example, I dont want to allow making a SubTask without a parent.
		//  How should i implement this, so the user can add this gaurd on a IssueTypeDefinition at runtime?
		// issue.validateCanRemoveParent();

		issue.removeParentIssue();

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
