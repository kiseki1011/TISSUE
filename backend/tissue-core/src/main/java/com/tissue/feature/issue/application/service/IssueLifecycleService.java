package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.request.BatchSoftDeleteCommand;
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
    private final ProjectMemberFinder projectMemberFinder;
    private final CustomFieldSchemaProcessor customFieldSchemaProcessor;
    private final IssueValidator issueValidator;
    private final IssueCommandRepository issueCommandRepository;
    private final IssueAuthorizationService issueAuthorizationService;
    private final IssueEventPublisher eventPublisher;

    @Override
    public IssueCreateResponse create(ProjectIdentifier pid, CreateIssueCommand cmd, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);

        // TODO: getWithProjectAndWorkflow를 만들어서 사용해야할까? Issue를 생성할때 currentState를 가져오기 위해서
        //  issueType.getWorkflow().getInitialState()로 탐색을 진행하기 때문에, Workflow도 같이 영속성 컨텍스트로 가져와야 할것 같은데
        IssueType issueType = issueTypeFinder.getWithProjectBy(pid.workspaceKey(), pid.projectKey(), cmd.issueTypeId());

        Project project = projectFinder.getWithLockBy(pid.workspaceKey(), pid.projectKey());

        Sprint sprint = Optional.ofNullable(cmd.sprintId())
                .map(id -> sprintFinder.getBy(id, project))
                .orElse(null);

        Issue parent = Optional.ofNullable(cmd.parentKey())
                .map(parentKey -> issueFinder.getWithProjectBy(pid.workspaceKey(), parentKey))
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

        customFieldSchemaProcessor.validateAndAssign(cmd.customFields(), issue);
        issueCommandRepository.save(issue);

        eventPublisher.publishIssueCreated(issue, actor);

        return IssueCreateResponse.from(issue);
    }

    @Override
    public void delete(IssueIdentifier iid, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(iid.workspaceKey(), iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey());

        issueAuthorizationService.requireIssueDeletePermission(issue, actor);
        issueValidator.ensureCanDelete(issue);

        issue.delete();

        eventPublisher.publishIssueDeleted(issue, actor);
    }

    @Override
    public void restore(IssueIdentifier iid, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(iid.workspaceKey(), iid.projectKey(), actorMemberId);

        Issue issue = issueFinder.getDeletedWithProjectBy(iid.workspaceKey(), iid.issueKey());

        issueAuthorizationService.requireIssueDeletePermission(issue, actor);

        issue.restoreSoftDeleted();

        eventPublisher.publishIssueRestored(issue, actor);
    }

    @Override
    public BatchOperationResponse batchSoftDelete(
            ProjectIdentifier pid, BatchSoftDeleteCommand cmd, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);

        List<Issue> issues = issueFinder.getAllBy(cmd.issueKeys(), pid.workspaceKey());
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
