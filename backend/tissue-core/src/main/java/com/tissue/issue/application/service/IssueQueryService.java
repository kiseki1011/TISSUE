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
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.ProjectMember;
import com.tissue.workflow.domain.Workflow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// TODO: Consider optimization
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

    @Override
    public IssueBasicInfo getBasic(String issueKey, ProjectMemberContext actorContext) {
        String workspaceKey = actorContext.workspaceKey();

        Issue issue = issueQueryRepo
                .findWithBasicInfo(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));

        ProjectMember author = projectMemberFinder.getBy(issue.getProject(), issue.getCreatedBy());
        ProjectMember updatedBy = projectMemberFinder.getBy(issue.getProject(), issue.getLastModifiedBy());

        return IssueBasicInfo.from(issue, author, updatedBy);
    }

    @Override
    public IssueCommonDetail getCommon(String issueKey, ProjectMemberContext actorContext) {
        String workspaceKey = actorContext.workspaceKey();

        Issue issue = issueQueryRepo
                .findWithDetail(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));

        ProjectMember author = projectMemberFinder.getBy(issue.getProject(), issue.getCreatedBy());
        ProjectMember updatedBy = projectMemberFinder.getBy(issue.getProject(), issue.getLastModifiedBy());
        List<IssueReviewer> reviewers = reviewerQueryRepo.findByIssue(workspaceKey, issueKey);

        return IssueCommonDetail.from(issue, author, updatedBy, reviewers);
    }

    @Override
    public IssueCustomDetail getCustom(String issueKey, ProjectMemberContext actorContext) {
        String workspaceKey = actorContext.workspaceKey();

        Issue issue = issueQueryRepo
                .findWithBasicInfo(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));

        List<IssueFieldValue> fieldValues =
                issueFieldValueQueryRepo.findByWorkspaceKeyAndIssueKey(workspaceKey, issueKey);

        return IssueCustomDetail.from(issue, fieldValues);
    }

    // TODO: Is this really needed?
    @Override
    public IssueIdentifierResponse getParent(String issueKey, ProjectMemberContext actorContext) {
        Issue issue = issueQueryRepo
                .findWithParent(actorContext.workspaceKey(), issueKey)
                .orElseThrow(() -> new IssueNotFoundException(actorContext.workspaceKey(), issueKey));

        Issue parent = issue.getParentIssue();
        if (parent == null) {
            return IssueIdentifierResponse.asNull();
        }

        return IssueIdentifierResponse.from(parent);
    }

    @Override
    public List<IssueIdentifierResponse> getChildren(String issueKey, ProjectMemberContext actorContext) {
        List<Issue> children = issueQueryRepo.findChildren(actorContext.workspaceKey(), issueKey);

        return children.stream().map(IssueIdentifierResponse::from).toList();
    }

    @Override
    public IssueRelationsDetail getRelations(String issueKey, ProjectMemberContext actorContext) {
        List<IssueRelation> allRelations = relationQueryRepo.findAllRelations(actorContext.workspaceKey(), issueKey);

        List<IssueRelation> outgoing = allRelations.stream()
                .filter(r -> r.getSourceIssue().getKey().equals(issueKey))
                .toList();

        List<IssueRelation> incoming = allRelations.stream()
                .filter(r -> r.getTargetIssue().getKey().equals(issueKey))
                .toList();

        return IssueRelationsDetail.from(outgoing, incoming);
    }

    @Override
    public ParticipantInfo getAuthor(String issueKey, ProjectMemberContext actorContext) {
        String workspaceKey = actorContext.workspaceKey();
        Issue issue = issueQueryRepo
                .findWithBasicInfo(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));

        ProjectMember author = projectMemberFinder.getBy(issue.getProject(), issue.getCreatedBy());

        return ParticipantInfo.from(author);
    }

    @Override
    public IssueReviewersDetail getReviewers(String issueKey, ProjectMemberContext actorContext) {
        List<IssueReviewer> reviewers = reviewerQueryRepo.findByIssue(actorContext.workspaceKey(), issueKey);
        return IssueReviewersDetail.from(reviewers);
    }

    @Override
    public IssueSubscribersDetail getSubscribers(String issueKey, ProjectMemberContext actorContext) {
        List<IssueSubscriber> subscribers = subscriberQueryRepo.findByIssue(actorContext.workspaceKey(), issueKey);
        return IssueSubscribersDetail.from(subscribers);
    }

    @Override
    public List<TransitionDetail> getAvailableTransitions(String issueKey, ProjectMemberContext actorContext) {
        String workspaceKey = actorContext.workspaceKey();
        Issue issue = issueQueryRepo
                .findWithBasicInfo(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));

        Workflow workflow = issue.getIssueType().getWorkflow();

        return workflow.getTransitions().stream()
                .filter(t -> t.getSourceState().equals(issue.getCurrentState()))
                .map(TransitionDetail::from)
                .toList();
    }
}
