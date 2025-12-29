package com.tissue.issue.application.service;

import com.tissue.issue.application.dto.response.IssueCommonDetail;
import com.tissue.issue.application.dto.response.IssueCustomDetail;
import com.tissue.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.issue.application.dto.response.IssueReviewersDetail;
import com.tissue.issue.application.dto.response.IssueSubscribersDetail;
import com.tissue.issue.application.dto.response.TransitionDetail;
import com.tissue.issue.application.dto.response.info.IssueBasicInfo;
import com.tissue.issue.application.dto.response.info.IssueIdentificationInfo;
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
import com.tissue.issue.domain.exception.IssueExceptions;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.workflow.domain.Workflow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueQueryService implements IssueQueryUseCase {

    private final IssueQueryRepository issueQueryRepo;
    private final IssueFieldValueQueryRepository issueFieldValueQueryRepo;
    private final IssueSubscriberQueryRepository subscriberQueryRepo;
    private final IssueReviewerQueryRepository reviewerQueryRepo;
    private final IssueRelationQueryRepository relationQueryRepo;
    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;

    @Override
    public IssueBasicInfo getBasic(String workspaceKey, String projectKey, String issueKey) {
        Issue issue = issueQueryRepo
                .findWithBasicInfo(workspaceKey, issueKey)
                .orElseThrow(() -> IssueExceptions.notFound(workspaceKey, issueKey));

        Project project = projectFinder.getBy(projectKey, workspaceKey);

        ProjectMember author = projectMemberFinder.findBy(project, issue.getCreatedBy());
        ProjectMember updatedBy = projectMemberFinder.findBy(project, issue.getLastModifiedBy());

        return IssueBasicInfo.from(issue, author, updatedBy);
    }

    @Override
    public IssueCommonDetail getCommon(String workspaceKey, String projectKey, String issueKey) {
        Issue issue = issueQueryRepo
                .findWithDetail(workspaceKey, issueKey)
                .orElseThrow(() -> IssueExceptions.notFound(workspaceKey, issueKey));

        Project project = projectFinder.getBy(projectKey, workspaceKey);

        ProjectMember author = projectMemberFinder.findBy(project, issue.getCreatedBy());
        ProjectMember updatedBy = projectMemberFinder.findBy(project, issue.getLastModifiedBy());
        List<IssueReviewer> reviewers = reviewerQueryRepo.findByIssue(workspaceKey, issueKey);

        return IssueCommonDetail.from(issue, author, updatedBy, reviewers);
    }

    @Override
    public IssueCustomDetail getCustom(String workspaceKey, String projectKey, String issueKey) {
        Issue issue = issueQueryRepo
                .findWithBasicInfo(workspaceKey, issueKey)
                .orElseThrow(() -> IssueExceptions.notFound(workspaceKey, issueKey));

        List<IssueFieldValue> fieldValues =
                issueFieldValueQueryRepo.findByWorkspaceKeyAndIssueKey(workspaceKey, issueKey);

        return IssueCustomDetail.from(issue, fieldValues);
    }

    @Override
    public IssueIdentificationInfo getParent(String workspaceKey, String projectKey, String issueKey) {
        Issue issue = issueQueryRepo
                .findWithParent(workspaceKey, issueKey)
                .orElseThrow(() -> IssueExceptions.notFound(workspaceKey, issueKey));

        Issue parent = issue.getParentIssue();
        if (parent == null) {
            return IssueIdentificationInfo.asNull();
        }

        return IssueIdentificationInfo.from(parent);
    }

    @Override
    public List<IssueIdentificationInfo> getChildren(String workspaceKey, String projectKey, String issueKey) {
        List<Issue> children = issueQueryRepo.findChildren(workspaceKey, issueKey);

        return children.stream().map(IssueIdentificationInfo::from).toList();
    }

    @Override
    public IssueRelationsDetail getRelations(String workspaceKey, String projectKey, String issueKey) {
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
        Issue issue = issueQueryRepo
                .findWithBasicInfo(workspaceKey, issueKey)
                .orElseThrow(() -> IssueExceptions.notFound(workspaceKey, issueKey));

        Project project = projectFinder.getBy(projectKey, workspaceKey);
        ProjectMember author = projectMemberFinder.findBy(project, issue.getCreatedBy());

        return ParticipantInfo.from(author);
    }

    @Override
    public IssueReviewersDetail getReviewers(String workspaceKey, String projectKey, String issueKey) {
        List<IssueReviewer> reviewers = reviewerQueryRepo.findByIssue(workspaceKey, issueKey);
        return IssueReviewersDetail.from(reviewers);
    }

    @Override
    public IssueSubscribersDetail getSubscribers(String workspaceKey, String projectKey, String issueKey) {
        List<IssueSubscriber> subscribers = subscriberQueryRepo.findByIssue(workspaceKey, issueKey);
        return IssueSubscribersDetail.from(subscribers);
    }

    @Override
    public List<TransitionDetail> getAvailableTransitions(String workspaceKey, String projectKey, String issueKey) {
        Issue issue = issueQueryRepo
                .findWithBasicInfo(issueKey, workspaceKey)
                .orElseThrow(() -> IssueExceptions.notFound(workspaceKey, issueKey));

        Workflow workflow = issue.getIssueType().getWorkflow();

        return workflow.getTransitions().stream()
                .filter(t -> t.getSourceState().equals(issue.getCurrentState()))
                .map(TransitionDetail::from)
                .toList();
    }
}
