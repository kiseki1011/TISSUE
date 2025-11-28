package com.tissue.api.issue.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.issue.application.dto.request.AssignParentCommand;
import com.tissue.api.issue.application.dto.request.CreateIssueCommand;
import com.tissue.api.issue.application.dto.request.DeleteIssueCommand;
import com.tissue.api.issue.application.dto.request.RemoveParentCommand;
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
import com.tissue.api.project.application.service.finder.ProjectFinder;
import com.tissue.api.project.application.service.finder.ProjectMemberFinder;
import com.tissue.api.project.domain.Project;
import com.tissue.api.project.domain.ProjectMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueCommandService implements IssueCommandUseCase {

	private final IssueFinder issueFinder;
	private final IssueTypeFinder issueTypeFinder;
	private final ProjectMemberFinder projectMemberFinder;
	private final ProjectFinder projectFinder;

	private final EpicStoryPointSyncService epicStoryPointSyncService;
	private final IssueProgressSyncService issueProgressSyncService;
	private final IssueFieldSchemaValidator fieldSchemaValidator;
	private final IssueValidator issueValidator;

	private final IssueCommandRepository issueCommandRepository;
	private final IssueFieldValueCommandRepository fieldValueRepository;

	@Override
	public IssueCommandResult create(CreateIssueCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		ProjectMember actor = projectMemberFinder.findBy(project, cmd.memberId());

		Issue issue = issueCommandRepository.save(Issue.create(
			project,
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

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), cmd.workspaceKey());

		Patchers.apply(cmd.title(), issue::updateTitle);
		Patchers.apply(cmd.content(), issue::updateContent);
		Patchers.apply(cmd.summary(), issue::updateSummary);
		Patchers.apply(cmd.dueAt(), issue::updateDueAt);
		Patchers.apply(cmd.priority(), issue::updatePriority);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult updateStoryPoint(UpdateStoryPointCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), cmd.workspaceKey());

		issue.updateStoryPoint(cmd.storyPoint());

		// TODO: 이벤트 발행 후, 리스너에서 처리 고려
		epicStoryPointSyncService.recalculateStoryPoint(issue.getParentIssue());
		issueProgressSyncService.recalculateProgress(issue.getParentIssue());

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult updateCustomFields(UpdateCustomFieldsCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), cmd.workspaceKey());

		List<IssueFieldValue> updateValues = fieldSchemaValidator.validateAndApplyPatch(
			cmd.customFields(),
			issue
		);

		fieldValueRepository.saveAll(updateValues);

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult assignParent(AssignParentCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), cmd.workspaceKey());
		Issue parent = issueFinder.findBy(cmd.parentIssueKey(), cmd.workspaceKey());

		issue.setParentIssue(parent);

		// TODO: 이벤트 발행 후, 리스너에서 처리 고려
		epicStoryPointSyncService.recalculateStoryPoint(issue.getParentIssue());
		issueProgressSyncService.recalculateProgress(issue.getParentIssue());

		return IssueCommandResult.from(issue);
	}

	@Override
	public IssueCommandResult removeParent(RemoveParentCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), cmd.workspaceKey());
		Issue parent = issue.getParentIssue();

		issue.removeParentIssue();

		// TODO: 이벤트 발행 후, 리스너에서 처리 고려
		epicStoryPointSyncService.recalculateStoryPoint(parent);
		issueProgressSyncService.recalculateProgress(parent);

		return IssueCommandResult.from(issue);
	}

	// TODO: 이슈 soft-delete에 대한 정책 수립이 필요
	@Override
	public IssueCommandResult softDelete(DeleteIssueCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), cmd.workspaceKey());
		Issue parent = issue.getParentIssue();

		issueValidator.ensureCanDelete(issue);
		issue.softDelete();

		// TODO: 이벤트 발행 후, 리스너에서 처리 고려
		epicStoryPointSyncService.recalculateStoryPoint(parent);
		issueProgressSyncService.recalculateProgress(parent);

		return IssueCommandResult.from(issue);
	}
}
