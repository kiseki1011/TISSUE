package com.tissue.api.issue.application.service;

import static com.tissue.api.common.util.IssueKeyUtil.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.issue.application.dto.request.AssignParentCommand;
import com.tissue.api.issue.application.dto.request.CreateIssueCommand;
import com.tissue.api.issue.application.dto.request.DeleteIssueCommand;
import com.tissue.api.issue.application.dto.request.RemoveParentCommand;
import com.tissue.api.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateCustomFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateStoryPointCommand;
import com.tissue.api.issue.application.dto.response.IssueCreateResponse;
import com.tissue.api.issue.application.port.in.IssueCommandUseCase;
import com.tissue.api.issue.application.port.out.IssueCommandRepository;
import com.tissue.api.issue.application.service.finder.IssueFinder;
import com.tissue.api.issue.application.service.validator.IssueFieldSchemaValidator;
import com.tissue.api.issue.application.service.validator.IssueValidator;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueContent;
import com.tissue.api.issue.domain.IssueParticipants;
import com.tissue.api.issue.domain.IssueSchedule;
import com.tissue.api.issue.domain.service.sync.EpicStoryPointSyncService;
import com.tissue.api.issue.domain.service.sync.IssueProgressSyncService;
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

	@Override
	@Transactional
	public IssueCreateResponse create(CreateIssueCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		ProjectMember actor = projectMemberFinder.findBy(project, cmd.memberId());

		Issue issue = Issue.create(
			project,
			issueType,
			cmd.title(),
			IssueContent.of(cmd.content(), cmd.summary()),
			IssueSchedule.of(cmd.dueAt()),
			IssueParticipants.of(actor),
			cmd.priority(),
			cmd.storyPoint()
		);

		fieldSchemaValidator.validateAndAssign(cmd.customFields(), issue);

		issueCommandRepository.save(issue);

		// TODO: IssueCreatedEvent
		//  대상: author(creator), reporter

		return IssueCreateResponse.from(issue);
	}

	@Override
	@Transactional
	public void updateCommonFields(UpdateCommonFieldsCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);

		Patchers.apply(cmd.title(), issue::updateTitle);
		Patchers.apply(cmd.content(), issue::updateContent);
		Patchers.apply(cmd.summary(), issue::updateSummary);
		Patchers.apply(cmd.dueAt(), issue::updateDueAt);
		Patchers.apply(cmd.priority(), issue::updatePriority);
	}

	// TODO: IssueHierarchy.STANDARD만 허용하는 검증 로직을 IssuePolicy로 분리 고려
	//  - 현재는 updateStoryPoint 내부에 응집 시킴
	@Override
	@Transactional
	public void updateStoryPoint(UpdateStoryPointCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);

		issue.updateStoryPoint(cmd.storyPoint());

		// TODO: StoryPointUpdatedEvent
		//  - 대상: author, reporter, assignee
		epicStoryPointSyncService.recalculateStoryPoint(issue.getParentIssue());
		issueProgressSyncService.recalculateProgress(issue.getParentIssue());
	}

	@Override
	@Transactional
	public void updateCustomFields(UpdateCustomFieldsCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);

		fieldSchemaValidator.validateAndApplyPatch(cmd.customFields(), issue);
	}

	// TODO: 부모가 EPIC인 경우만 cross project 허용
	@Override
	@Transactional
	public void assignParent(AssignParentCommand cmd) {
		Project childProject = projectFinder.findForCommand(extractProjectKey(cmd.issueKey()), cmd.workspaceKey());
		Issue child = issueFinder.findBy(cmd.issueKey(), childProject);
		Project parentProject = projectFinder.findForCommand(extractProjectKey(cmd.parentIssueKey()),
			cmd.workspaceKey());
		Issue parent = issueFinder.findBy(cmd.parentIssueKey(), parentProject);

		child.setParentIssue(parent);

		// TODO: IssueParentAssignedEvent
		//  - 대상: author, reporter, assignee
		//  - 대상2: 부모 이슈의 author, reporter, assignee
		epicStoryPointSyncService.recalculateStoryPoint(child.getParentIssue());
		issueProgressSyncService.recalculateProgress(child.getParentIssue());
	}

	@Override
	@Transactional
	public void removeParent(RemoveParentCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);
		Issue parent = issue.getParentIssue();

		issue.removeParentIssue();

		// TODO: IssueParentRemovedEvent
		//  - 대상: author, reporter, assignee
		//  - 대상2: 부모 이슈의 author, reporter, assignee
		epicStoryPointSyncService.recalculateStoryPoint(parent);
		issueProgressSyncService.recalculateProgress(parent);
	}

	@Override
	@Transactional
	public void softDelete(DeleteIssueCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);
		Issue parent = issue.getParentIssue();

		issueValidator.ensureCanDelete(issue);
		issue.delete();

		// TODO: IssueDeletedEvent
		//  - 대상: author, reporter, assignee, subscribers, reviewers, 프로젝트의 ADMIN들
		epicStoryPointSyncService.recalculateStoryPoint(parent);
		issueProgressSyncService.recalculateProgress(parent);
	}
}
