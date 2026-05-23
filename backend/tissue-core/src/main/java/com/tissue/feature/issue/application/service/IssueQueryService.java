package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.response.IssueCommonDetail;
import com.tissue.feature.issue.application.dto.response.IssueCustomDetail;
import com.tissue.feature.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.feature.issue.application.dto.response.IssueReviewersDetail;
import com.tissue.feature.issue.application.dto.response.IssueSubscribersDetail;
import com.tissue.feature.issue.application.dto.response.TransitionDetail;
import com.tissue.feature.issue.application.dto.response.info.CustomFieldValueInfo;
import com.tissue.feature.issue.application.dto.response.info.IssueBasicInfo;
import com.tissue.feature.issue.application.dto.response.info.IssueIdentifierResponse;
import com.tissue.feature.issue.application.dto.response.info.ParticipantInfo;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.application.port.repository.IssueRelationQueryRepository;
import com.tissue.feature.issue.application.port.repository.IssueReviewerQueryRepository;
import com.tissue.feature.issue.application.port.repository.IssueSubscriberQueryRepository;
import com.tissue.feature.issue.application.port.usecase.IssueQueryUseCase;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueRelation;
import com.tissue.feature.issue.domain.IssueReviewer;
import com.tissue.feature.issue.domain.IssueSubscriber;
import com.tissue.feature.issue.domain.exception.IssueNotFoundException;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.service.TransitionGuardEvaluator;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueQueryService implements IssueQueryUseCase {

    private final IssueFinder issueFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueQueryRepository issueQueryRepository;
    private final IssueRelationQueryRepository issueRelationQueryRepository;
    private final IssueReviewerQueryRepository issueReviewerQueryRepository;
    private final IssueSubscriberQueryRepository issueSubscriberQueryRepository;
    private final TransitionGuardEvaluator transitionGuardEvaluator;

    @Override
    public IssueBasicInfo getBasic(IssueIdentifier iid, Long actorMemberId) {
        Issue issue = issueQueryRepository
                .findWithBasicInfo(iid.workspaceKey(), iid.issueKey())
                .orElseThrow(() -> new IssueNotFoundException(iid.workspaceKey(), iid.issueKey()));

        projectMemberFinder.getBy(issue.getProject(), actorMemberId);

        ProjectMember author = projectMemberFinder
                .findOptionalIncludingSoftDeleted(issue.getProject(), issue.getCreatedBy())
                .orElse(null);
        ProjectMember updatedBy = projectMemberFinder
                .findOptionalIncludingSoftDeleted(issue.getProject(), issue.getLastModifiedBy())
                .orElse(null);

        return IssueBasicInfo.from(issue, author, updatedBy);
    }

    @Override
    public IssueCommonDetail getCommonFieldValues(IssueIdentifier iid, Long actorMemberId) {
        Issue issue = issueFinder.getWithProjectAndIssueTypeBy(iid.workspaceKey(), iid.issueKey());

        projectMemberFinder.getBy(issue.getProject(), actorMemberId);

        ProjectMember author = projectMemberFinder
                .findOptionalIncludingSoftDeleted(issue.getProject(), issue.getCreatedBy())
                .orElse(null);
        ProjectMember updatedBy = projectMemberFinder
                .findOptionalIncludingSoftDeleted(issue.getProject(), issue.getLastModifiedBy())
                .orElse(null);
        List<IssueReviewer> reviewers = issueReviewerQueryRepository.findByIssue(iid.workspaceKey(), iid.issueKey());

        return IssueCommonDetail.from(issue, author, updatedBy, reviewers);
    }

    @Override
    public IssueCustomDetail getCustomFieldValues(IssueIdentifier iid, Long actorMemberId) {
        Issue issue = issueFinder.getWithProjectAndIssueTypeBy(iid.workspaceKey(), iid.issueKey());

        projectMemberFinder.getBy(issue.getProject(), actorMemberId);

        List<CustomFieldValueInfo> customFields = issue.getIssueType().getFields().stream()
                .map(field ->
                        toCustomFieldValueInfo(field, issue.getCustomFields().get(String.valueOf(field.getId()))))
                .toList();

        return new IssueCustomDetail(issue.getKey(), customFields);
    }

    @Override
    public IssueIdentifierResponse getParent(IssueIdentifier iid, Long actorMemberId) {
        Issue issue = issueQueryRepository
                .findWithParent(iid.workspaceKey(), iid.issueKey())
                .orElseThrow(() -> new IssueNotFoundException(iid.workspaceKey(), iid.issueKey()));

        projectMemberFinder.getBy(issue.getProject(), actorMemberId);

        Issue parent = issue.getParentIssue();
        return parent == null ? IssueIdentifierResponse.asNull() : IssueIdentifierResponse.from(parent);
    }

    @Override
    public List<IssueIdentifierResponse> getChildren(IssueIdentifier iid, Long actorMemberId) {
        Issue issue = issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey());

        projectMemberFinder.getBy(issue.getProject(), actorMemberId);

        return issueQueryRepository.findChildren(iid.workspaceKey(), iid.issueKey()).stream()
                .map(IssueIdentifierResponse::from)
                .toList();
    }

    @Override
    public IssueRelationsDetail getRelations(IssueIdentifier iid, Long actorMemberId) {
        Issue issue = issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey());

        projectMemberFinder.getBy(issue.getProject(), actorMemberId);

        List<IssueRelation> outgoing =
                issueRelationQueryRepository.findBySourceIssue(iid.workspaceKey(), iid.issueKey());
        List<IssueRelation> incoming =
                issueRelationQueryRepository.findByTargetIssue(iid.workspaceKey(), iid.issueKey());

        return IssueRelationsDetail.from(outgoing, incoming);
    }

    @Override
    public ParticipantInfo getAuthor(IssueIdentifier iid, Long actorMemberId) {
        Issue issue = issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey());

        projectMemberFinder.getBy(issue.getProject(), actorMemberId);

        ProjectMember author = projectMemberFinder
                .findOptionalIncludingSoftDeleted(issue.getProject(), issue.getCreatedBy())
                .orElse(null);
        return ParticipantInfo.from(author);
    }

    @Override
    public IssueReviewersDetail getReviewers(IssueIdentifier iid, Long actorMemberId) {
        Issue issue = issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey());

        projectMemberFinder.getBy(issue.getProject(), actorMemberId);

        List<IssueReviewer> reviewers = issueReviewerQueryRepository.findByIssue(iid.workspaceKey(), iid.issueKey());

        return IssueReviewersDetail.from(reviewers);
    }

    @Override
    public IssueSubscribersDetail getSubscribers(IssueIdentifier iid, Long actorMemberId) {
        Issue issue = issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey());

        projectMemberFinder.getBy(issue.getProject(), actorMemberId);

        List<IssueSubscriber> subscribers =
                issueSubscriberQueryRepository.findByIssue(iid.workspaceKey(), iid.issueKey());

        return IssueSubscribersDetail.from(subscribers);
    }

    @Override
    public List<TransitionDetail> getAvailableTransitions(IssueIdentifier iid, Long actorMemberId) {
        Issue issue = issueFinder.getWithProjectAndIssueTypeBy(iid.workspaceKey(), iid.issueKey());

        projectMemberFinder.getBy(issue.getProject(), actorMemberId);

        Workflow workflow = issue.getIssueType().getWorkflow();
        WorkflowState currentState = issue.getCurrentState();

        return workflow.getTransitions().stream()
                .filter(t -> t.getSourceState().getId().equals(currentState.getId()))
                .map(t -> TransitionDetail.from(t, transitionGuardEvaluator.collectViolations(issue, t, actorMemberId)))
                .toList();
    }

    private CustomFieldValueInfo toCustomFieldValueInfo(IssueField field, @Nullable Object rawValue) {
        return new CustomFieldValueInfo(
                field.getId(), field.getName(), field.getIssueFieldType(), field.isRequired(), rawValue);
    }
}
