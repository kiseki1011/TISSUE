package com.tissue.issue.application.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.common.dto.FieldChange;
import com.tissue.common.util.Patchers;
import com.tissue.issue.application.dto.request.AssignParentCommand;
import com.tissue.issue.application.dto.request.CreateIssueCommand;
import com.tissue.issue.application.dto.request.DeleteIssueCommand;
import com.tissue.issue.application.dto.request.RemoveParentCommand;
import com.tissue.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.issue.application.dto.request.UpdateCustomFieldsCommand;
import com.tissue.issue.application.dto.request.UpdateStoryPointCommand;
import com.tissue.issue.application.dto.response.IssueCreateResponse;
import com.tissue.issue.application.port.in.IssueCommandUseCase;
import com.tissue.issue.application.port.out.IssueCommandRepository;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.application.service.validator.IssueFieldSchemaValidator;
import com.tissue.issue.application.service.validator.IssueValidator;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueContent;
import com.tissue.issue.domain.IssueParticipants;
import com.tissue.issue.domain.IssueSchedule;
import com.tissue.issue.domain.event.IssueCreatedEvent;
import com.tissue.issue.domain.event.IssueDeletedEvent;
import com.tissue.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.issue.domain.event.IssueParentChangedEvent;
import com.tissue.issue.domain.event.IssueStoryPointChangedEvent;
import com.tissue.issue.domain.service.IssueFieldChangeTracker;
import com.tissue.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.sprint.application.service.finder.SprintFinder;
import com.tissue.sprint.domain.Sprint;

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
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);
		ProjectMember actor = projectMemberFinder.findBy(project, cmd.actorMemberId());

		Sprint sprint = Optional.ofNullable(cmd.sprintId())
			.map(id -> sprintFinder.findBy(id, project))
			.orElse(null);

		Issue parent = Optional.ofNullable(cmd.parentKey())
			.map(parentKey -> resolveParentIssue(parentKey, cmd.parentProjectKey(), project))
			.orElse(null);

		ProjectMember assignee = Optional.ofNullable(cmd.assigneeMemberId())
			.map(id -> projectMemberFinder.findBy(project, id))
			.orElse(null);

		Issue issue = Issue.create(
			project,
			sprint,
			issueType,
			cmd.title(),
			IssueContent.of(cmd.content(), cmd.summary()),
			IssueSchedule.of(cmd.dueAt()),
			IssueParticipants.of(actor, assignee),
			cmd.priority(),
			cmd.storyPoint(),
			parent
		);

		fieldSchemaValidator.validateAndAssign(cmd.customFields(), issue);
		issueCommandRepository.save(issue);

		eventPublisher.publishEvent(IssueCreatedEvent.create(issue, actor));

		return IssueCreateResponse.from(issue);
	}

	@Override
	@Transactional
	public void updateCommonFields(UpdateCommonFieldsCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);
		ProjectMember actor = projectMemberFinder.findBy(project, cmd.actorMemberId());

		Map<String, FieldChange> changes = new HashMap<>();

		Patchers.applyWithLog(cmd.title(), issue::getTitle, issue::updateTitle, "title", changes);
		Patchers.applyWithLog(cmd.content(), issue::getContent, issue::updateContent, "content", changes);
		Patchers.applyWithLog(cmd.summary(), issue::getSummary, issue::updateSummary, "summary", changes);
		Patchers.applyWithLog(cmd.dueAt(), () -> issue.getSchedule().getDueAt(), issue::updateDueAt, "dueAt", changes);
		Patchers.applyWithLog(cmd.priority(), issue::getPriority, issue::updatePriority, "priority", changes);

		if (!changes.isEmpty()) {
			eventPublisher.publishEvent(IssueFieldsUpdatedEvent.create(issue, changes, actor));
		}
	}

	@Override
	@Transactional
	public void updateCustomFields(UpdateCustomFieldsCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);
		ProjectMember actor = projectMemberFinder.findBy(project, cmd.actorMemberId());

		Map<String, Object> oldSnapshot = fieldChangeTracker.captureSnapshot(issue);

		fieldSchemaValidator.validateAndApplyPatch(cmd.customFields(), issue);

		Map<String, Object> newSnapshot = fieldChangeTracker.captureSnapshot(issue);
		Map<String, FieldChange> changes = fieldChangeTracker.compareChanges(oldSnapshot, newSnapshot);

		if (!changes.isEmpty()) {
			eventPublisher.publishEvent(IssueFieldsUpdatedEvent.create(issue, changes, actor));
		}
	}

	@Override
	@Transactional
	public void updateStoryPoint(UpdateStoryPointCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);
		ProjectMember actor = projectMemberFinder.findBy(project, cmd.actorMemberId());

		Integer oldStoryPoint = issue.getStoryPoint();
		issue.updateStoryPoint(cmd.storyPoint());

		eventPublisher.publishEvent(
			IssueStoryPointChangedEvent.create(issue, issue.getParentIssue(), oldStoryPoint, actor)
		);
	}

	@Override
	@Transactional
	public void assignParent(AssignParentCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);
		ProjectMember actor = projectMemberFinder.findBy(project, cmd.actorMemberId());

		Project parentProject = projectFinder.getModifiableBy(cmd.parentProjectKey(), cmd.workspaceKey());
		Issue parent = issueFinder.findBy(cmd.parentIssueKey(), parentProject);

		Issue oldParent = issue.getParentIssue();

		issue.setParentIssue(parent);

		eventPublisher.publishEvent(IssueParentChangedEvent.create(issue, oldParent, parent, actor));
	}

	@Override
	@Transactional
	public void removeParent(RemoveParentCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);
		Issue parent = issue.getParentIssue();
		ProjectMember actor = projectMemberFinder.findBy(project, cmd.actorMemberId());

		issue.removeParentIssue();

		eventPublisher.publishEvent(IssueParentChangedEvent.create(issue, parent, null, actor));
	}

	@Override
	@Transactional
	public void softDelete(DeleteIssueCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);
		ProjectMember actor = projectMemberFinder.findBy(project, cmd.actorMemberId());

		issueValidator.ensureCanDelete(issue);
		issue.delete();

		eventPublisher.publishEvent(IssueDeletedEvent.create(issue, actor));
	}

	private Issue resolveParentIssue(String parentKey, String parentProjectKey, Project currentProject) {
		Project targetProject = currentProject;

		if (parentProjectKey != null && !parentProjectKey.equals(currentProject.getKey())) {
			targetProject = projectFinder.getModifiableBy(parentProjectKey, currentProject.getWorkspaceKey());
		}

		return issueFinder.findBy(parentKey, targetProject);
	}
}
