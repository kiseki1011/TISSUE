package com.tissue.api.issue.application.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.dto.FieldChange;
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
import com.tissue.api.issue.domain.event.IssueCreatedEvent;
import com.tissue.api.issue.domain.event.IssueDeletedEvent;
import com.tissue.api.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.api.issue.domain.event.IssueParentChangedEvent;
import com.tissue.api.issue.domain.event.IssueStoryPointChangedEvent;
import com.tissue.api.issue.domain.service.IssueFieldChangeTracker;
import com.tissue.api.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.project.application.service.finder.ProjectFinder;
import com.tissue.api.project.application.service.finder.ProjectMemberFinder;
import com.tissue.api.project.domain.Project;
import com.tissue.api.project.domain.ProjectMember;
import com.tissue.api.sprint.application.service.finder.SprintFinder;
import com.tissue.api.sprint.domain.Sprint;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueCommandService implements IssueCommandUseCase {

	private final IssueFinder issueFinder;
	private final IssueTypeFinder issueTypeFinder;
	private final SprintFinder sprintFinder;
	private final ProjectFinder projectFinder;
	private final ProjectMemberFinder projectMemberFinder;
	private final IssueFieldSchemaValidator fieldSchemaValidator;
	private final IssueValidator issueValidator;
	private final IssueFieldChangeTracker fieldChangeTracker;
	private final IssueCommandRepository issueCommandRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional
	public IssueCreateResponse create(CreateIssueCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		ProjectMember actor = projectMemberFinder.findBy(project, cmd.memberId());

		Sprint sprint = sprintFinder.findOptBy(cmd.sprintId(), project)
			.orElse(null);

		// TODO: 만약 parentProjectKey가 null 이라면? 만약 parentKey가 null이라면?
		Issue parent = Optional.ofNullable(cmd.parentKey())
			.map(parentKey -> resolveParentIssue(parentKey, cmd.parentProjectKey(), project))
			.orElse(null);

		Issue issue = Issue.create(
			project,
			sprint,
			issueType,
			cmd.title(),
			IssueContent.of(cmd.content(), cmd.summary()),
			IssueSchedule.of(cmd.dueAt()),
			IssueParticipants.of(actor),
			cmd.priority(),
			cmd.storyPoint(),
			parent
		);

		fieldSchemaValidator.validateAndAssign(cmd.customFields(), issue);
		issueCommandRepository.save(issue);

		eventPublisher.publishEvent(IssueCreatedEvent.create(issue, cmd.memberId()));

		return IssueCreateResponse.from(issue);
	}

	@Override
	@Transactional
	public void updateCommonFields(UpdateCommonFieldsCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);

		Map<String, FieldChange> changes = new HashMap<>();

		Patchers.applyWithLog(cmd.title(), issue::getTitle, issue::updateTitle, "title", changes);
		Patchers.applyWithLog(cmd.content(), issue::getContent, issue::updateContent, "content", changes);
		Patchers.applyWithLog(cmd.summary(), issue::getSummary, issue::updateSummary, "summary", changes);
		Patchers.applyWithLog(cmd.dueAt(), () -> issue.getSchedule().getDueAt(), issue::updateDueAt, "dueAt", changes);
		Patchers.applyWithLog(cmd.priority(), issue::getPriority, issue::updatePriority, "priority", changes);

		if (!changes.isEmpty()) {
			eventPublisher.publishEvent(IssueFieldsUpdatedEvent.create(
				issue,
				changes,
				cmd.memberId()
			));
		}
	}

	@Override
	@Transactional
	public void updateCustomFields(UpdateCustomFieldsCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);

		Map<String, Object> oldSnapshot = fieldChangeTracker.captureSnapshot(issue);

		fieldSchemaValidator.validateAndApplyPatch(cmd.customFields(), issue);

		Map<String, Object> newSnapshot = fieldChangeTracker.captureSnapshot(issue);
		Map<String, FieldChange> changes = fieldChangeTracker.compareChanges(oldSnapshot, newSnapshot);

		if (!changes.isEmpty()) {
			eventPublisher.publishEvent(IssueFieldsUpdatedEvent.create(
				issue,
				changes,
				cmd.memberId()
			));
		}
	}

	@Override
	@Transactional
	public void updateStoryPoint(UpdateStoryPointCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);

		Integer oldStoryPoint = issue.getStoryPoint();
		issue.updateStoryPoint(cmd.storyPoint());

		eventPublisher.publishEvent(IssueStoryPointChangedEvent.create(
			issue,
			issue.getParentIssue(),
			oldStoryPoint,
			cmd.memberId())
		);
	}

	// TODO: assignParent과 removeParent를 통합하는게 나으려나?
	@Override
	@Transactional
	public void assignParent(AssignParentCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);

		Project parentProject = projectFinder.findForCommand(cmd.parentProjectKey(), cmd.workspaceKey());
		Issue parent = issueFinder.findBy(cmd.parentIssueKey(), parentProject);

		Issue oldParent = issue.getParentIssue();

		issue.setParentIssue(parent);

		eventPublisher.publishEvent(IssueParentChangedEvent.create(
			issue,
			oldParent,
			parent,
			cmd.memberId())
		);
	}

	@Override
	@Transactional
	public void removeParent(RemoveParentCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);
		Issue parent = issue.getParentIssue();

		issue.removeParentIssue();

		eventPublisher.publishEvent(IssueParentChangedEvent.create(
			issue,
			parent,
			null,
			cmd.memberId())
		);
	}

	@Override
	@Transactional
	public void softDelete(DeleteIssueCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);

		issueValidator.ensureCanDelete(issue);
		issue.delete();

		eventPublisher.publishEvent(IssueDeletedEvent.create(issue, cmd.memberId()));
	}

	private Issue resolveParentIssue(String parentKey, String parentProjectKey, Project currentProject) {
		Project targetProject = currentProject;

		if (parentProjectKey != null && !parentProjectKey.equals(currentProject.getKey())) {
			targetProject = projectFinder.findForCommand(parentProjectKey, currentProject.getWorkspaceKey());
		}

		return issueFinder.findBy(parentKey, targetProject);
	}
}
