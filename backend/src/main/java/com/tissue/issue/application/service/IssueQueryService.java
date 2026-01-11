package com.tissue.issue.application.service;

import com.tissue.issue.application.dto.response.IssueCommonDetail;
import com.tissue.issue.application.dto.response.IssueCustomDetail;
import com.tissue.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.issue.application.dto.response.IssueReviewersDetail;
import com.tissue.issue.application.dto.response.IssueSubscribersDetail;
import com.tissue.issue.application.dto.response.TransitionDetail;
import com.tissue.issue.application.dto.response.info.IssueBasicInfo;
import com.tissue.issue.application.dto.response.info.IssueIdentifierResponse;
import com.tissue.issue.application.dto.response.info.ParticipantInfo;
import com.tissue.issue.application.port.in.IssueQueryUseCase;
import com.tissue.issue.application.port.out.IssueFieldValueQueryRepository;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.application.port.out.IssueRelationQueryRepository;
import com.tissue.issue.application.port.out.IssueReviewerQueryRepository;
import com.tissue.issue.application.port.out.IssueSubscriberQueryRepository;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueFieldValue;
import com.tissue.issue.domain.IssueRelation;
import com.tissue.issue.domain.IssueReviewer;
import com.tissue.issue.domain.IssueSubscriber;
import com.tissue.issue.domain.exception.IssueNotFoundException;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.security.authentication.application.port.out.CurrentMemberProvider;
import com.tissue.workflow.domain.Workflow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueQueryService implements IssueQueryUseCase {

    private final IssueQueryRepository issueQueryRepo;
    private final IssueFieldValueQueryRepository issueFieldValueQueryRepo;
    private final IssueSubscriberQueryRepository subscriberQueryRepo;
    private final IssueReviewerQueryRepository reviewerQueryRepo;
    private final IssueRelationQueryRepository relationQueryRepo;
    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final ProjectAuthorizationService projectAuthService;
    private final CurrentMemberProvider currentMemberProvider;

    @Override
    public IssueBasicInfo getBasic(String workspaceKey, String projectKey, String issueKey) {
        requireProjectViewer(workspaceKey, projectKey);

        Issue issue = issueQueryRepo
                .findWithBasicInfo(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));

        Project project = projectFinder.getBy(projectKey, workspaceKey);

        ProjectMember author = projectMemberFinder.getBy(project, issue.getCreatedBy());
        ProjectMember updatedBy = projectMemberFinder.getBy(project, issue.getLastModifiedBy());

        return IssueBasicInfo.from(issue, author, updatedBy);
    }

    @Override
    public IssueCommonDetail getCommon(String workspaceKey, String projectKey, String issueKey) {
        requireProjectViewer(workspaceKey, projectKey);

        Issue issue = issueQueryRepo
                .findWithDetail(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));

        Project project = projectFinder.getBy(projectKey, workspaceKey);

        ProjectMember author = projectMemberFinder.getBy(project, issue.getCreatedBy());
        ProjectMember updatedBy = projectMemberFinder.getBy(project, issue.getLastModifiedBy());
        List<IssueReviewer> reviewers = reviewerQueryRepo.findByIssue(workspaceKey, issueKey);

        return IssueCommonDetail.from(issue, author, updatedBy, reviewers);
    }

    @Override
    public IssueCustomDetail getCustom(String workspaceKey, String projectKey, String issueKey) {
        requireProjectViewer(workspaceKey, projectKey);

        Issue issue = issueQueryRepo
                .findWithBasicInfo(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));

        List<IssueFieldValue> fieldValues =
                issueFieldValueQueryRepo.findByWorkspaceKeyAndIssueKey(workspaceKey, issueKey);

        return IssueCustomDetail.from(issue, fieldValues);
    }

    @Override
    public IssueIdentifierResponse getParent(String workspaceKey, String projectKey, String issueKey) {
        requireProjectViewer(workspaceKey, projectKey);

        Issue issue = issueQueryRepo
                .findWithParent(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));

        Issue parent = issue.getParentIssue();
        if (parent == null) {
            return IssueIdentifierResponse.asNull();
        }

        return IssueIdentifierResponse.from(parent);
    }

    @Override
    public List<IssueIdentifierResponse> getChildren(String workspaceKey, String projectKey, String issueKey) {
        requireProjectViewer(workspaceKey, projectKey);

        List<Issue> children = issueQueryRepo.findChildren(workspaceKey, issueKey);

        return children.stream().map(IssueIdentifierResponse::from).toList();
    }

    @Override
    public IssueRelationsDetail getRelations(String workspaceKey, String projectKey, String issueKey) {
        requireProjectViewer(workspaceKey, projectKey);

        List<IssueRelation> allRelations = relationQueryRepo.findAllRelations(workspaceKey, issueKey);

        List<IssueRelation> outgoing = allRelations.stream()
                .filter(r -> r.getSourceIssue().getKey().equals(issueKey))
                .toList();

        List<IssueRelation> incoming = allRelations.stream()
                .filter(r -> r.getTargetIssue().getKey().equals(issueKey))
                .toList();

        return IssueRelationsDetail.from(outgoing, incoming);
    }

    @Override
    public ParticipantInfo getAuthor(String workspaceKey, String projectKey, String issueKey) {
        requireProjectViewer(workspaceKey, projectKey);

        Issue issue = issueQueryRepo
                .findWithBasicInfo(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));

        Project project = projectFinder.getBy(projectKey, workspaceKey);
        ProjectMember author = projectMemberFinder.getBy(project, issue.getCreatedBy());

        return ParticipantInfo.from(author);
    }

    @Override
    public IssueReviewersDetail getReviewers(String workspaceKey, String projectKey, String issueKey) {
        requireProjectViewer(workspaceKey, projectKey);

        List<IssueReviewer> reviewers = reviewerQueryRepo.findByIssue(workspaceKey, issueKey);
        return IssueReviewersDetail.from(reviewers);
    }

    @Override
    public IssueSubscribersDetail getSubscribers(String workspaceKey, String projectKey, String issueKey) {
        requireProjectViewer(workspaceKey, projectKey);

        List<IssueSubscriber> subscribers = subscriberQueryRepo.findByIssue(workspaceKey, issueKey);
        return IssueSubscribersDetail.from(subscribers);
    }

    @Override
    public List<TransitionDetail> getAvailableTransitions(String workspaceKey, String projectKey, String issueKey) {
        requireProjectViewer(workspaceKey, projectKey);

        Issue issue = issueQueryRepo
                .findWithBasicInfo(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));

        Workflow workflow = issue.getIssueType().getWorkflow();

        return workflow.getTransitions().stream()
                .filter(t -> t.getSourceState().equals(issue.getCurrentState()))
                .map(TransitionDetail::from)
                .toList();
    }

    private void requireProjectViewer(String workspaceKey, String projectKey) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        projectAuthService.requireProjectViewer(workspaceKey, projectKey, actorMemberId);
    }
}
