package com.tissue.api.issue.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.issue.application.dto.request.CreateIssueCommand;
import com.tissue.api.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateCustomFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateStoryPointCommand;
import com.tissue.api.issue.application.dto.response.IssueCommandResult;
import com.tissue.api.issue.application.port.in.IssueCommandUseCase;
import com.tissue.api.issue.application.service.finder.IssueFinder;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueContent;
import com.tissue.api.issue.domain.IssueFieldValue;
import com.tissue.api.issue.domain.IssueParticipants;
import com.tissue.api.issue.domain.IssueSchedule;
import com.tissue.api.issue.domain.port.out.IssueCommandRepository;
import com.tissue.api.issue.domain.port.out.IssueFieldValueCommandRepository;
import com.tissue.api.issue.domain.service.sync.EpicStoryPointSyncService;
import com.tissue.api.issue.domain.service.sync.IssueProgressSyncService;
import com.tissue.api.issue.domain.service.validator.IssueFieldSchemaValidator;
import com.tissue.api.issue.domain.service.validator.IssueValidator;
import com.tissue.api.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.api.workspace.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueCommandService implements IssueCommandUseCase {

	private final IssueFinder issueFinder;
	private final IssueTypeFinder issueTypeFinder;
	private final WorkspaceFinder workspaceFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;

	private final EpicStoryPointSyncService epicStoryPointSyncService;
	private final IssueProgressSyncService issueProgressSyncService;
	private final IssueFieldSchemaValidator fieldSchemaValidator;
	private final IssueValidator issueValidator;

	private final IssueCommandRepository issueCommandRepository;
	private final IssueFieldValueCommandRepository fieldValueRepository;

	@Override
	public IssueCommandResult create(CreateIssueCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findByIdAndWorkspace(cmd.issueTypeId(), workspace);
		WorkspaceMember actor = workspaceMemberFinder.findByMemberIdAndWorkspaceKey(cmd.memberId(), cmd.workspaceKey());

		Issue issue = issueCommandRepository.save(Issue.create(
			workspace,
			issueType,
			cmd.title(),
			IssueContent.of(cmd.content(), cmd.summary()),
			IssueSchedule.of(cmd.dueAt()),
			IssueParticipants.of(actor),
			cmd.priority(),
			cmd.storyPoint()
		));

		List<IssueFieldValue> values = fieldSchemaValidator.validateAndExtract(cmd.customFields(), issue);
		fieldValueRepository.saveAll(values);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult updateCommonFields(UpdateCommonFieldsCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceKey());

		Patchers.apply(cmd.title(), issue::updateTitle);
		Patchers.apply(cmd.content(), issue::updateContent);
		Patchers.apply(cmd.summary(), issue::updateSummary);
		Patchers.apply(cmd.dueAt(), issue::updateDueAt);
		Patchers.apply(cmd.priority(), issue::updatePriority);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult updateStoryPoint(UpdateStoryPointCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceKey());

		issue.updateStoryPoint(cmd.storyPoint());

		// TODO: 이벤트 발행 후, 리스너에서 처리 고려
		epicStoryPointSyncService.recalculateStoryPoint(issue.getParentIssue());
		issueProgressSyncService.recalculateProgress(issue.getParentIssue());

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult updateCustomFields(UpdateCustomFieldsCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceKey());

		List<IssueFieldValue> updateValues = fieldSchemaValidator.validateAndApplyPatch(
			cmd.customFields(),
			issue
		);
		fieldValueRepository.saveAll(updateValues);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult assignParent(String workspaceKey, String issueKey, String parentIssueKey) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		Issue parent = issueFinder.findIssue(parentIssueKey, workspaceKey);

		issue.setParentIssue(parent);

		// TODO: 이벤트 발행 후, 리스너에서 처리 고려
		epicStoryPointSyncService.recalculateStoryPoint(issue.getParentIssue());
		issueProgressSyncService.recalculateProgress(issue.getParentIssue());

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult removeParent(String workspaceKey, String issueKey) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		Issue parent = issue.getParentIssue();

		issue.removeParentIssue();

		// TODO: 이벤트 발행 후, 리스너에서 처리 고려
		epicStoryPointSyncService.recalculateStoryPoint(parent);
		issueProgressSyncService.recalculateProgress(parent);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult softDelete(String workspaceKey, String issueKey) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		Issue parent = issue.getParentIssue();

		issueValidator.ensureCanDelete(issue);
		issue.softDelete();

		// TODO: 이벤트 발행 후, 리스너에서 처리 고려
		epicStoryPointSyncService.recalculateStoryPoint(parent);
		issueProgressSyncService.recalculateProgress(parent);

		return IssueCommandResult.from(issue);
	}
}
