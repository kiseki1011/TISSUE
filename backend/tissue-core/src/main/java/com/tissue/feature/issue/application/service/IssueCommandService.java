package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.feature.issue.application.dto.response.IssueCreateResponse;
import com.tissue.feature.issue.application.port.repository.IssueCommandRepository;
import com.tissue.feature.issue.application.port.usecase.IssueCommandUseCase;
import com.tissue.feature.issue.application.service.authorization.IssueAuthorizationService;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.application.service.validator.IssueFieldSchemaValidator;
import com.tissue.feature.issue.application.service.validator.IssueValidator;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueContent;
import com.tissue.feature.issue.domain.IssueParticipants;
import com.tissue.feature.issue.domain.IssueSchedule;
import com.tissue.feature.issue.domain.service.IssueFieldChangeTracker;
import com.tissue.feature.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.sprint.application.service.SprintFinder;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.shared.dto.FieldChange;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.support.util.Patchers;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
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
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final IssueFieldSchemaValidator fieldSchemaValidator;
    private final IssueValidator issueValidator;
    private final IssueFieldChangeTracker fieldChangeTracker;
    private final IssueCommandRepository issueCommandRepository;
    private final IssueEventPublisher eventPublisher;
    private final IssueAuthorizationService issueAuthService;

    @Override
    public IssueCreateResponse create(ProjectIdentifier projectIdentifier, CreateIssueCommand cmd, Long memberId) {
        IssueType issueType = issueTypeFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), cmd.issueTypeId());
        Project project = issueType.getProject();

        Sprint sprint = Optional.ofNullable(cmd.sprintId())
                .map(id -> sprintFinder.getBy(id, project))
                .orElse(null);

        Issue parent = Optional.ofNullable(cmd.parentKey())
                .map(parentKey -> resolveParentIssue(parentKey, cmd.parentProjectKey(), project))
                .orElse(null);

        ProjectMember assignee = Optional.ofNullable(cmd.assigneeMemberId())
                .map(id -> projectMemberFinder.getBy(project, id))
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

        WorkspaceMember actor = workspaceMemberFinder.getBy(projectIdentifier.workspaceKey(), memberId);
        eventPublisher.publishIssueCreated(issue, actor);

        return IssueCreateResponse.from(issue);
    }

    // TODO: Needs Javadoc to explain the logic
    @Override
    public void updateCommonFields(IssueIdentifier issueIdentifier, UpdateCommonFieldsCommand cmd, Long memberId) {
        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        Map<String, FieldChange> changes = new HashMap<>();

        Patchers.applyWithLog(cmd.title(), issue::getTitle, issue::updateTitle, "title", changes);
        Patchers.applyWithLog(cmd.content(), issue::getContent, issue::updateContent, "content", changes);
        Patchers.applyWithLog(cmd.summary(), issue::getSummary, issue::updateSummary, "summary", changes);
        Patchers.applyWithLog(cmd.dueAt(), () -> issue.getSchedule().getDueAt(), issue::updateDueAt, "dueAt", changes);
        Patchers.applyWithLog(cmd.priority(), issue::getPriority, issue::updatePriority, "priority", changes);

        if (!changes.isEmpty()) {
            WorkspaceMember actor = workspaceMemberFinder.getBy(issueIdentifier.workspaceKey(), memberId);
            eventPublisher.publishIssueFieldsUpdated(issue, changes, actor);
        }
    }

    @Override
    public void updateCustomFields(IssueIdentifier issueIdentifier, Map<Long, Object> customFields, Long memberId) {
        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        Map<String, Object> oldSnapshot = fieldChangeTracker.captureSnapshot(issue);

        fieldSchemaValidator.validateAndApplyPatch(customFields, issue);

        Map<String, Object> newSnapshot = fieldChangeTracker.captureSnapshot(issue);
        Map<String, FieldChange> changes = fieldChangeTracker.compareChanges(oldSnapshot, newSnapshot);

        if (!changes.isEmpty()) {
            WorkspaceMember actor = workspaceMemberFinder.getBy(issueIdentifier.workspaceKey(), memberId);
            eventPublisher.publishIssueFieldsUpdated(issue, changes, actor);
        }
    }

    @Override
    public void updateStoryPoint(IssueIdentifier issueIdentifier, @Nullable Integer storyPoint, Long memberId) {
        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        Integer oldStoryPoint = issue.getStoryPoint();
        issue.updateStoryPoint(storyPoint);

        WorkspaceMember actor = workspaceMemberFinder.getBy(issueIdentifier.workspaceKey(), memberId);
        eventPublisher.publishStoryPointChanged(issue, oldStoryPoint, actor);
    }

    @Override
    public void assignParent(IssueIdentifier issueIdentifier, String parentIssueKey, Long memberId) {
        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        Issue newParent = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), parentIssueKey);
        Issue oldParent = issue.getParentIssue();

        issue.setParentIssue(newParent);

        WorkspaceMember actor = workspaceMemberFinder.getBy(issueIdentifier.workspaceKey(), memberId);
        eventPublisher.publishParentChanged(issue, oldParent, newParent, actor);
    }

    @Override
    public void removeParent(IssueIdentifier issueIdentifier, Long memberId) {
        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        Issue parent = issue.getParentIssue();
        if (parent == null) {
            return;
        }

        issue.removeParentIssue();

        WorkspaceMember actor = workspaceMemberFinder.getBy(issueIdentifier.workspaceKey(), memberId);
        eventPublisher.publishParentChanged(issue, parent, null, actor);
    }

    @Override
    public void delete(IssueIdentifier issueIdentifier, Long memberId) {
        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        WorkspaceMember actor = workspaceMemberFinder.getBy(issueIdentifier.workspaceKey(), memberId);

        // TODO: workspace admin or project creator or issue creator
        issueAuthService.requireIssueDeletePermission(issue, actor);
        issueValidator.ensureCanDelete(issue);

        issue.delete();

        eventPublisher.publishIssueDeleted(issue, actor);
    }

    private Issue resolveParentIssue(String parentKey, String parentProjectKey, Project currentProject) {
        Project targetProject = currentProject;

        if (parentProjectKey != null && !parentProjectKey.equals(currentProject.getKey())) {
            targetProject = projectFinder.getWithWorkspaceBy(currentProject.getWorkspaceKey(), parentProjectKey);
        }

        return issueFinder.getWithProjectBy(targetProject.getWorkspaceKey(), parentKey);
    }
}
