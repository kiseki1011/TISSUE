package com.tissue.api.issue.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.AssignParentIssueCommand;
import com.tissue.api.issue.application.dto.CreateIssueCommand;
import com.tissue.api.issue.application.dto.RemoveParentIssueCommand;
import com.tissue.api.issue.application.finder.IssueFinder;
import com.tissue.api.issue.application.finder.IssueTypeFinder;
import com.tissue.api.issue.application.validator.IssueFieldSchemaValidator;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.IssueFieldValue;
import com.tissue.api.issue.infrastructure.repository.IssueFieldValueRepository;
import com.tissue.api.issue.infrastructure.repository.IssueRepository;
import com.tissue.api.issue.presentation.dto.response.IssueResponse;
import com.tissue.api.issuetype.domain.IssueType;
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
 *  - Issue Update(meta-data update)
 *  - Issue Soft-Delete
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

	@Transactional
	public IssueResponse create(CreateIssueCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findIssueType(workspace, cmd.issueTypeId());
		WorkspaceMember actor = workspaceMemberFinder.findWorkspaceMember(cmd.currentMemberId(), cmd.workspaceKey());

		Issue issue = issueRepository.save(Issue.create(
			workspace,
			issueType,
			cmd.title(),
			cmd.content(),
			cmd.summary(),
			cmd.priority(),
			cmd.dueAt(),
			cmd.storyPoint()
		));

		List<IssueFieldValue> values = fieldSchemaValidator.validateAndExtract(cmd.customFields(), issue);
		fieldValueRepository.saveAll(values);

		// TODO: 아래 로직을 Issue.create에 캡슐화 하는게 좋을까?
		issue.updateReporter(actor); // TODO: updateReporter 대신 setReporter가 더 나으려나?
		issue.addWatcher(actor);

		return IssueResponse.from(issue);
	}

	// TODO: updateReporter도 추가
	// @Transactional
	// public IssueResponse update(UpdateIssueCommand cmd) {
	// 	Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceCode());
	//
	// 	if (cmd.title() != null) {
	// 		issue.updateTitle(cmd.title());
	// 	}
	// 	if (cmd.content() != null) {
	// 		issue.updateContent(cmd.content());
	// 	}
	// 	if (cmd.summary() != null) {
	// 		issue.updateSummary(cmd.summary());
	// 	}
	// 	if (cmd.priority() != null) {
	// 		issue.updatePriority(cmd.priority());
	// 	}
	// 	if (cmd.dueAt() != null) {
	// 		issue.updateDueAt(cmd.dueAt());
	// 	}
	//
	// 	if (cmd.customFields() != null && !cmd.customFields().isEmpty()) {
	// 		List<IssueFieldValue> updateValues = fieldSchemaValidator.validateAndApplyPartialUpdate(
	// 			cmd.customFields(),
	// 			issue
	// 		);
	// 		fieldValueRepository.saveAll(updateValues);
	// 	}
	//
	// 	return IssueResponse.from(issue);
	// }

	@Transactional
	public IssueResponse assignParent(AssignParentIssueCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceCode());
		Issue parent = issueFinder.findIssue(cmd.parentIssueKey(), cmd.workspaceCode());

		issue.assignParentIssue(parent);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse removeParent(RemoveParentIssueCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceCode());

		issue.removeParentIssue();

		return IssueResponse.from(issue);
	}

	// @Transactional
	// public void softDelete(
	// 	String workspaceKey,
	// 	String issueKey,
	// 	Long requesterWorkspaceMemberId
	// ) {
	// 	Issue issue = issueReader.findIssue(issueKey, workspaceKey);
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
