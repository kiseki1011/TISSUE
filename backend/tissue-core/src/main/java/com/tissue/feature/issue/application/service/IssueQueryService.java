package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.response.IssueCommonDetail;
import com.tissue.feature.issue.application.dto.response.IssueCustomDetail;
import com.tissue.feature.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.feature.issue.application.dto.response.IssueReviewersDetail;
import com.tissue.feature.issue.application.dto.response.IssueSubscribersDetail;
import com.tissue.feature.issue.application.dto.response.TransitionDetail;
import com.tissue.feature.issue.application.dto.response.info.IssueBasicInfo;
import com.tissue.feature.issue.application.dto.response.info.IssueIdentifierResponse;
import com.tissue.feature.issue.application.dto.response.info.ParticipantInfo;
import com.tissue.feature.issue.application.port.repository.IssueFieldValueQueryRepository;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.application.port.repository.IssueRelationQueryRepository;
import com.tissue.feature.issue.application.port.repository.IssueReviewerQueryRepository;
import com.tissue.feature.issue.application.port.repository.IssueSubscriberQueryRepository;
import com.tissue.feature.issue.application.port.usecase.IssueQueryUseCase;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueFieldValue;
import com.tissue.feature.issue.domain.IssueRelation;
import com.tissue.feature.issue.domain.IssueReviewer;
import com.tissue.feature.issue.domain.IssueSubscriber;
import com.tissue.feature.issue.domain.exception.IssueNotFoundException;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.shared.dto.IssueIdentifier;
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
    private final ProjectMemberFinder projectMemberFinder;

    @Override
    public IssueBasicInfo getBasic(IssueIdentifier issueIdentifier, Long memberId) {
        String workspaceKey = issueIdentifier.workspaceKey();
        String issueKey = issueIdentifier.issueKey();

        Issue issue = issueQueryRepo
                .findWithBasicInfo(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));

        ProjectMember author = projectMemberFinder.getBy(issue.getProject(), issue.getCreatedBy());
        ProjectMember updatedBy = projectMemberFinder.getBy(issue.getProject(), issue.getLastModifiedBy());

        return IssueBasicInfo.from(issue, author, updatedBy);
    }

    @Override
    public IssueCommonDetail getCommon(IssueIdentifier issueIdentifier, Long memberId) {
        String workspaceKey = issueIdentifier.workspaceKey();
        String issueKey = issueIdentifier.issueKey();

        Issue issue = issueQueryRepo
                .findWithDetail(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));

        ProjectMember author = projectMemberFinder.getBy(issue.getProject(), issue.getCreatedBy());
        ProjectMember updatedBy = projectMemberFinder.getBy(issue.getProject(), issue.getLastModifiedBy());
        List<IssueReviewer> reviewers = reviewerQueryRepo.findByIssue(workspaceKey, issueKey);

        return IssueCommonDetail.from(issue, author, updatedBy, reviewers);
    }

    @Override
    public IssueCustomDetail getCustom(IssueIdentifier issueIdentifier, Long memberId) {
        String workspaceKey = issueIdentifier.workspaceKey();
        String issueKey = issueIdentifier.issueKey();

        Issue issue = issueQueryRepo
                .findWithBasicInfo(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));

        List<IssueFieldValue> fieldValues =
                issueFieldValueQueryRepo.findByWorkspaceKeyAndIssueKey(workspaceKey, issueKey);

        return IssueCustomDetail.from(issue, fieldValues);
    }

    @Override
    public IssueIdentifierResponse getParent(IssueIdentifier issueIdentifier, Long memberId) {
        Issue issue = issueQueryRepo
                .findWithParent(issueIdentifier.workspaceKey(), issueIdentifier.issueKey())
                .orElseThrow(
                        () -> new IssueNotFoundException(issueIdentifier.workspaceKey(), issueIdentifier.issueKey()));

        Issue parent = issue.getParentIssue();
        if (parent == null) {
            return IssueIdentifierResponse.asNull();
        }

        return IssueIdentifierResponse.from(parent);
    }

    @Override
    public List<IssueIdentifierResponse> getChildren(IssueIdentifier issueIdentifier, Long memberId) {
        List<Issue> children = issueQueryRepo.findChildren(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        return children.stream().map(IssueIdentifierResponse::from).toList();
    }

    @Override
    public IssueRelationsDetail getRelations(IssueIdentifier issueIdentifier, Long memberId) {
        List<IssueRelation> allRelations =
                relationQueryRepo.findAllRelations(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        List<IssueRelation> outgoing = allRelations.stream()
                .filter(r -> r.getSourceIssue().getKey().equals(issueIdentifier.issueKey()))
                .toList();

        List<IssueRelation> incoming = allRelations.stream()
                .filter(r -> r.getTargetIssue().getKey().equals(issueIdentifier.issueKey()))
                .toList();

        return IssueRelationsDetail.from(outgoing, incoming);
    }

    @Override
    public ParticipantInfo getAuthor(IssueIdentifier issueIdentifier, Long memberId) {
        String workspaceKey = issueIdentifier.workspaceKey();
        String issueKey = issueIdentifier.issueKey();

        Issue issue = issueQueryRepo
                .findWithBasicInfo(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));

        ProjectMember author = projectMemberFinder.getBy(issue.getProject(), issue.getCreatedBy());

        return ParticipantInfo.from(author);
    }

    @Override
    public IssueReviewersDetail getReviewers(IssueIdentifier issueIdentifier, Long memberId) {
        List<IssueReviewer> reviewers =
                reviewerQueryRepo.findByIssue(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());
        return IssueReviewersDetail.from(reviewers);
    }

    @Override
    public IssueSubscribersDetail getSubscribers(IssueIdentifier issueIdentifier, Long memberId) {
        List<IssueSubscriber> subscribers =
                subscriberQueryRepo.findByIssue(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());
        return IssueSubscribersDetail.from(subscribers);
    }

    @Override
    public List<TransitionDetail> getAvailableTransitions(IssueIdentifier issueIdentifier, Long memberId) {
        String workspaceKey = issueIdentifier.workspaceKey();
        String issueKey = issueIdentifier.issueKey();

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
