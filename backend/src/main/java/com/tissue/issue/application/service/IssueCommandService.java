package com.tissue.issue.application.service;

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
import com.tissue.issue.application.service.authorization.IssueAuthorizationService;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.issue.application.service.validator.IssueFieldSchemaValidator;
import com.tissue.issue.application.service.validator.IssueValidator;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueContent;
import com.tissue.issue.domain.IssueParticipants;
import com.tissue.issue.domain.IssueSchedule;
import com.tissue.issue.domain.service.IssueFieldChangeTracker;
import com.tissue.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.sprint.application.service.SprintFinder;
import com.tissue.sprint.domain.Sprint;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
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
    private final IssueEventPublisher eventPublisher;
    private final ProjectAuthorizationService projectAuthService;
    private final IssueAuthorizationService issueAuthService;

    @Override
    public IssueCreateResponse create(CreateIssueCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        IssueType issueType = issueTypeFinder.getBy(cmd.issueTypeId(), project);

        projectAuthService.requireProjectMember(actorContext);

        Sprint sprint = Optional.ofNullable(cmd.sprintId())
                .map(id -> sprintFinder.getBy(id, project))
                .orElse(null);

        Issue parent = Optional.ofNullable(cmd.parentKey())
                .map(parentKey -> resolveParentIssue(parentKey, cmd.parentProjectKey(), project))
                .orElse(null);

        ProjectMember assignee = Optional.ofNullable(cmd.assigneeMemberId())
                .map(id -> projectMemberFinder.getActive(project, id))
                .orElse(null);

        Issue issue = Issue.create(
                project,
                sprint,
                issueType,
                cmd.title(),
                IssueContent.of(cmd.content(), cmd.summary()),
                IssueSchedule.of(cmd.dueAt()),
                IssueParticipants.of(assignee),
                cmd.priority(),
                cmd.storyPoint(),
                parent);

        fieldSchemaValidator.validateAndAssign(cmd.customFields(), issue);
        issueCommandRepository.save(issue);

        eventPublisher.publishIssueCreated(issue, actorContext);

        return IssueCreateResponse.from(issue);
    }

    // TODO: Needs Javadoc to explain the logic
    @Override
    public void updateCommonFields(UpdateCommonFieldsCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        issueAuthService.requireIssueEditPermission(issue, actorContext);

        Map<String, FieldChange> changes = new HashMap<>();

        Patchers.applyWithLog(cmd.title(), issue::getTitle, issue::updateTitle, "title", changes);
        Patchers.applyWithLog(cmd.content(), issue::getContent, issue::updateContent, "content", changes);
        Patchers.applyWithLog(cmd.summary(), issue::getSummary, issue::updateSummary, "summary", changes);
        Patchers.applyWithLog(cmd.dueAt(), () -> issue.getSchedule().getDueAt(), issue::updateDueAt, "dueAt", changes);
        Patchers.applyWithLog(cmd.priority(), issue::getPriority, issue::updatePriority, "priority", changes);

        if (!changes.isEmpty()) {
            eventPublisher.publishIssueFieldsUpdated(issue, changes, actorContext);
        }
    }

    @Override
    public void updateCustomFields(UpdateCustomFieldsCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        issueAuthService.requireIssueEditPermission(issue, actorContext);

        Map<String, Object> oldSnapshot = fieldChangeTracker.captureSnapshot(issue);

        fieldSchemaValidator.validateAndApplyPatch(cmd.customFields(), issue);

        Map<String, Object> newSnapshot = fieldChangeTracker.captureSnapshot(issue);
        Map<String, FieldChange> changes = fieldChangeTracker.compareChanges(oldSnapshot, newSnapshot);

        if (!changes.isEmpty()) {
            eventPublisher.publishIssueFieldsUpdated(issue, changes, actorContext);
        }
    }

    @Override
    public void updateStoryPoint(UpdateStoryPointCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        issueAuthService.requireIssueEditPermission(issue, actorContext);

        Integer oldStoryPoint = issue.getStoryPoint();
        issue.updateStoryPoint(cmd.storyPoint());

        eventPublisher.publishStoryPointChanged(issue, oldStoryPoint, actorContext);
    }

    @Override
    public void assignParent(AssignParentCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        issueAuthService.requireIssueEditPermission(issue, actorContext);

        Project parentProject = projectFinder.getModifiableBy(cmd.parentProjectKey(), actorContext.workspaceKey());
        Issue parent = issueFinder.getBy(cmd.parentIssueKey(), parentProject);

        Issue oldParent = issue.getParentIssue();

        issue.setParentIssue(parent);

        eventPublisher.publishParentChanged(issue, oldParent, parent, actorContext);
    }

    @Override
    public void removeParent(RemoveParentCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        issueAuthService.requireIssueEditPermission(issue, actorContext);

        Issue parent = issue.getParentIssue();
        if (parent == null) {
            return;
        }

        issue.removeParentIssue();

        eventPublisher.publishParentChanged(issue, parent, null, actorContext);
    }

    @Override
    public void softDelete(DeleteIssueCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        issueAuthService.requireIssueDeletePermission(issue, actorContext);
        issueValidator.ensureCanDelete(issue);

        issue.delete();

        eventPublisher.publishIssueDeleted(issue, actorContext);
    }

    private Issue resolveParentIssue(String parentKey, String parentProjectKey, Project currentProject) {
        Project targetProject = currentProject;

        if (parentProjectKey != null && !parentProjectKey.equals(currentProject.getKey())) {
            targetProject = projectFinder.getModifiableBy(parentProjectKey, currentProject.getWorkspaceKey());
        }

        return issueFinder.getBy(parentKey, targetProject);
    }
}
