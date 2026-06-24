package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.request.BatchDeleteCommand;
import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.dto.response.IssueCreateResponse;
import com.tissue.feature.issue.application.port.repository.IssueCommandRepository;
import com.tissue.feature.issue.application.port.usecase.IssueLifecycleUseCase;
import com.tissue.feature.issue.application.service.authorization.IssueAuthorizationService;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.application.service.validator.CustomFieldSchemaProcessor;
import com.tissue.feature.issue.application.service.validator.IssueValidator;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueContent;
import com.tissue.feature.issue.domain.IssueParticipants;
import com.tissue.feature.issue.domain.IssueSchedule;
import com.tissue.feature.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.application.service.finder.ProjectAccessResolver;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.sprint.application.service.SprintFinder;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.shared.dto.BatchOperationResponse;
import com.tissue.shared.dto.BatchOperationResponse.BatchFailure;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ForbiddenException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueLifecycleService implements IssueLifecycleUseCase {

    private final IssueFinder issueFinder;
    private final IssueTypeFinder issueTypeFinder;
    private final SprintFinder sprintFinder;
    private final ProjectFinder projectFinder;
    private final ProjectAccessResolver projectAccessResolver;
    private final ProjectMemberFinder projectMemberFinder;
    private final CustomFieldSchemaProcessor customFieldSchemaProcessor;
    private final IssueValidator issueValidator;
    private final IssueCommandRepository issueCommandRepository;
    private final IssueAuthorizationService issueAuthorizationService;
    private final IssueEventPublisher eventPublisher;

    @Override
    public IssueCreateResponse create(ProjectIdentifier pid, CreateIssueCommand cmd, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(pid.projectKey(), actorMemberId);

        IssueType issueType = issueTypeFinder.getWithWorkflowBy(cmd.issueTypeId());

        Project project = projectFinder.getWithLockByProjectKey(pid.projectKey());

        Sprint sprint = Optional.ofNullable(cmd.sprintId())
                .map(id -> sprintFinder.getBy(id, project))
                .orElse(null);

        Issue parent = Optional.ofNullable(cmd.parentKey())
                .map(issueFinder::getWithProjectByIssueKey)
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

        // A SUBTASK/MICROTASK can't be created standalone — it must be given a parent
        // (the domain factory stays permissive; the rule is enforced here at the API
        // boundary, the only production create path).
        issue.ensureParentPresentWhenRequired();

        customFieldSchemaProcessor.validateAndAssign(cmd.customFields(), issue);
        issueCommandRepository.save(issue);

        eventPublisher.publishIssueCreated(issue, actor);

        return IssueCreateResponse.from(issue);
    }

    @Override
    public void delete(IssueIdentifier iid, Long actorMemberId) {
        ProjectMember actor = projectAccessResolver.resolveByProjectKey(iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());

        issueAuthorizationService.requireIssueDeletePermission(issue, actor);
        issueValidator.ensureCanDelete(issue);

        issue.delete();

        eventPublisher.publishIssueDeleted(issue, actor);
    }

    @Override
    public void restore(IssueIdentifier iid, Long actorMemberId) {
        ProjectMember actor = projectAccessResolver.resolveByProjectKey(iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getDeletedWithProjectByIssueKey(iid.issueKey());

        issueAuthorizationService.requireIssueDeletePermission(issue, actor);

        issue.restoreSoftDeleted();

        eventPublisher.publishIssueRestored(issue, actor);
    }

    @Override
    public BatchOperationResponse batchDelete(ProjectIdentifier pid, BatchDeleteCommand cmd, Long actorMemberId) {
        ProjectMember actor = projectAccessResolver.resolveByProjectKey(pid.projectKey(), actorMemberId);

        List<Issue> issues = issueFinder.getAllByIssueKeys(cmd.issueKeys());
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
