package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.request.BatchChangeParentCommand;
import com.tissue.feature.issue.application.dto.request.BatchRemoveParentCommand;
import com.tissue.feature.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.feature.issue.application.port.usecase.IssueUpdateUseCase;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.application.service.validator.CustomFieldSchemaProcessor;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueFields;
import com.tissue.feature.issue.domain.service.IssueFieldChangeTracker;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueUpdateService implements IssueUpdateUseCase {

    private final IssueFinder issueFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final CustomFieldSchemaProcessor customFieldSchemaProcessor;
    private final IssueFieldChangeTracker fieldChangeTracker;
    private final IssueEventPublisher eventPublisher;

    @Override
    public void updateCommonFields(IssueIdentifier iid, UpdateCommonFieldsCommand cmd, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());

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
    public void updateCustomFields(IssueIdentifier iid, Map<Long, Object> customFields, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectIssueTypeAndFieldsByIssueKey(iid.issueKey());

        Map<String, Object> oldSnapshot = fieldChangeTracker.captureSnapshot(issue);

        customFieldSchemaProcessor.validateAndApplyPatch(customFields, issue);

        Map<String, Object> newSnapshot = fieldChangeTracker.captureSnapshot(issue);
        Map<String, String> fieldIdToName = issue.getIssueType().getFields().stream()
                .collect(Collectors.toMap(field -> String.valueOf(field.getId()), IssueField::getName));
        Map<String, FieldChange> changes = fieldChangeTracker.compareChanges(oldSnapshot, newSnapshot, fieldIdToName);

        if (!changes.isEmpty()) {
            eventPublisher.publishIssueFieldsUpdated(issue, changes, actor);
        }
    }

    @Override
    public void updateStoryPoint(IssueIdentifier iid, @Nullable Integer storyPoint, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());

        Integer oldStoryPoint = issue.getStoryPoint();
        issue.updateStoryPoint(storyPoint);

        eventPublisher.publishStoryPointChanged(issue, oldStoryPoint, actor);
    }

    @Override
    public void assignParent(IssueIdentifier iid, String parentIssueKey, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());

        Issue newParent = issueFinder.getWithProjectByIssueKey(parentIssueKey);
        Issue oldParent = issue.getParentIssue();

        issue.setParentIssue(newParent);

        eventPublisher.publishParentChanged(issue, oldParent, newParent, actor);
    }

    @Override
    public void removeParent(IssueIdentifier iid, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());

        Issue parent = issue.getParentIssue();
        if (parent == null) {
            return;
        }

        issue.removeParentIssue();

        eventPublisher.publishParentChanged(issue, parent, null, actor);
    }

    @Override
    public BatchOperationResponse batchAssignParent(
            ProjectIdentifier pid, BatchChangeParentCommand cmd, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(pid.projectKey(), actorMemberId);

        List<Issue> issues = issueFinder.getAllByIssueKeys(cmd.issueKeys());
        Issue newParent = issueFinder.getWithProjectByIssueKey(cmd.parentIssueKey());

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
    public BatchOperationResponse batchRemoveParent(
            ProjectIdentifier pid, BatchRemoveParentCommand cmd, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(pid.projectKey(), actorMemberId);

        List<Issue> issues = issueFinder.getAllByIssueKeys(cmd.issueKeys());
        List<BatchFailure> failures = new ArrayList<>();

        for (Issue issue : issues) {
            try {
                Issue parent = issue.getParentIssue();
                if (parent == null) {
                    continue;
                }

                issue.removeParentIssue();

                eventPublisher.publishParentChanged(issue, parent, null, actor);

            } catch (BadRequestException | ForbiddenException e) {
                failures.add(new BatchFailure(issue.getKey(), e.getMessage()));
            }
        }

        return BatchOperationResponse.of(issues.size(), failures);
    }
}
