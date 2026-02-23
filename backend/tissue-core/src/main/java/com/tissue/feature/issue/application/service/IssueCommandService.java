package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.request.BatchChangeParentCommand;
import com.tissue.feature.issue.application.dto.request.BatchSoftDeleteCommand;
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
import com.tissue.feature.issue.domain.IssueFields;
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
import com.tissue.shared.dto.BatchOperationResponse;
import com.tissue.shared.dto.BatchOperationResponse.BatchFailure;
import com.tissue.shared.dto.FieldChange;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ForbiddenException;
import com.tissue.support.util.Patchers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private final IssueFieldSchemaValidator fieldSchemaValidator;
    private final IssueValidator issueValidator;
    private final IssueFieldChangeTracker fieldChangeTracker;
    private final IssueCommandRepository issueCommandRepository;
    private final IssueAuthorizationService issueAuthorizationService;
    private final IssueEventPublisher eventPublisher;

    @Override
    public IssueCreateResponse create(ProjectIdentifier projectIdentifier, CreateIssueCommand cmd, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        IssueType issueType = issueTypeFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), cmd.issueTypeId());

        Project project = projectFinder.getWithLockBy(projectIdentifier.workspaceKey(), projectIdentifier.projectKey());

        Sprint sprint = Optional.ofNullable(cmd.sprintId())
                .map(id -> sprintFinder.getBy(id, project))
                .orElse(null);

        Issue parent = Optional.ofNullable(cmd.parentKey())
                .map(parentKey -> issueFinder.getWithProjectBy(projectIdentifier.workspaceKey(), parentKey))
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

        eventPublisher.publishIssueCreated(issue, actor);

        return IssueCreateResponse.from(issue);
    }

    @Override
    public void updateCommonFields(IssueIdentifier issueIdentifier, UpdateCommonFieldsCommand cmd, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                issueIdentifier.workspaceKey(), issueIdentifier.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        Map<String, FieldChange> changes = new HashMap<>();

        Patchers.applyWithLog(cmd.title(), issue::getTitle, issue::updateTitle, IssueFields.TITLE, changes);
        Patchers.applyWithLog(cmd.content(), issue::getContent, issue::updateContent, IssueFields.CONTENT, changes);
        Patchers.applyWithLog(cmd.summary(), issue::getSummary, issue::updateSummary, IssueFields.SUMMARY, changes);
        Patchers.applyWithLog(
                cmd.dueAt(), () -> issue.getSchedule().getDueAt(), issue::updateDueAt, IssueFields.DUE_AT, changes);
        Patchers.applyWithLog(cmd.priority(), issue::getPriority, issue::updatePriority, IssueFields.PRIORITY, changes);

        if (!changes.isEmpty()) {
            eventPublisher.publishIssueFieldsUpdated(issue, changes, actor);
        }
    }

    @Override
    public void updateCustomFields(
            IssueIdentifier issueIdentifier, Map<Long, Object> customFields, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                issueIdentifier.workspaceKey(), issueIdentifier.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        Map<String, Object> oldSnapshot = fieldChangeTracker.captureSnapshot(issue);

        fieldSchemaValidator.validateAndApplyPatch(customFields, issue);

        Map<String, Object> newSnapshot = fieldChangeTracker.captureSnapshot(issue);
        Map<String, FieldChange> changes = fieldChangeTracker.compareChanges(oldSnapshot, newSnapshot);

        if (!changes.isEmpty()) {
            eventPublisher.publishIssueFieldsUpdated(issue, changes, actor);
        }
    }

    @Override
    public void updateStoryPoint(IssueIdentifier issueIdentifier, @Nullable Integer storyPoint, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                issueIdentifier.workspaceKey(), issueIdentifier.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        Integer oldStoryPoint = issue.getStoryPoint();
        issue.updateStoryPoint(storyPoint);

        eventPublisher.publishStoryPointChanged(issue, oldStoryPoint, actor);
    }

    @Override
    public void assignParent(IssueIdentifier issueIdentifier, String parentIssueKey, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                issueIdentifier.workspaceKey(), issueIdentifier.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        Issue newParent = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), parentIssueKey);
        Issue oldParent = issue.getParentIssue();

        issue.setParentIssue(newParent);

        eventPublisher.publishParentChanged(issue, oldParent, newParent, actor);
    }

    @Override
    public void removeParent(IssueIdentifier issueIdentifier, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                issueIdentifier.workspaceKey(), issueIdentifier.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        Issue parent = issue.getParentIssue();
        if (parent == null) {
            return;
        }

        issue.removeParentIssue();

        eventPublisher.publishParentChanged(issue, parent, null, actor);
    }

    @Override
    public void delete(IssueIdentifier issueIdentifier, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                issueIdentifier.workspaceKey(), issueIdentifier.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        issueAuthorizationService.requireIssueDeletePermission(issue, actor);
        issueValidator.ensureCanDelete(issue);

        issue.delete();

        eventPublisher.publishIssueDeleted(issue, actor);
    }

    @Override
    public void restore(IssueIdentifier issueIdentifier, Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                issueIdentifier.workspaceKey(), issueIdentifier.projectKey(), actorMemberId);

        Issue issue = issueFinder.getDeletedWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        issueAuthorizationService.requireIssueDeletePermission(issue, actor);

        issue.restoreSoftDeleted();

        eventPublisher.publishIssueRestored(issue, actor);
    }

    @Override
    public BatchOperationResponse batchChangeParent(
            ProjectIdentifier projectIdentifier, BatchChangeParentCommand cmd, Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        List<Issue> issues = issueFinder.getAllBy(cmd.issueKeys(), projectIdentifier.workspaceKey());
        Issue newParent = issueFinder.getWithProjectBy(projectIdentifier.workspaceKey(), cmd.parentIssueKey());

        List<BatchFailure> failures = new ArrayList<>();

        for (Issue issue : issues) {
            try {
                Issue oldParent = issue.getParentIssue();
                issue.setParentIssue(newParent);

                eventPublisher.publishParentChanged(issue, oldParent, newParent, actor);

            } catch (BadRequestException | ForbiddenException e) {
                failures.add(new BatchFailure(issue.getKey(), e.getMessage()));
            }
        }

        return BatchOperationResponse.of(issues.size(), failures);
    }

    @Override
    public BatchOperationResponse batchSoftDelete(
            ProjectIdentifier projectIdentifier, BatchSoftDeleteCommand cmd, Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        List<Issue> issues = issueFinder.getAllBy(cmd.issueKeys(), projectIdentifier.workspaceKey());
        List<BatchFailure> failures = new ArrayList<>();

        for (Issue issue : issues) {
            try {
                issueAuthorizationService.requireIssueDeletePermission(issue, actor);
                issueValidator.ensureCanDelete(issue);

                issue.delete();

                eventPublisher.publishIssueDeleted(issue, actor);

            } catch (BadRequestException | ForbiddenException e) {
                failures.add(new BatchFailure(issue.getKey(), e.getMessage()));
            }
        }

        return BatchOperationResponse.of(issues.size(), failures);
    }
}
