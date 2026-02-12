package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.port.usecase.IssueRelationUseCase;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueRelation;
import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.feature.issue.domain.service.relation.RelationCycleDetector;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.shared.dto.IssueIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueRelationService implements IssueRelationUseCase {

    private final IssueFinder issueFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final RelationCycleDetector relationCycleDetector;
    private final IssueEventPublisher eventPublisher;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Override
    public void add(
            IssueIdentifier sourceIssueIdentifier,
            String targetIssueKey,
            IssueRelationType relationType,
            Long memberId) {

        Issue sourceIssue =
                issueFinder.getWithProjectBy(sourceIssueIdentifier.workspaceKey(), sourceIssueIdentifier.issueKey());
        Issue targetIssue = issueFinder.getWithProjectBy(sourceIssueIdentifier.workspaceKey(), targetIssueKey);

        projectAuthorizationService.requireProjectMember(targetIssue.getProject(), memberId);

        relationCycleDetector.ensureNoCycle(sourceIssue, targetIssue, relationType);
        IssueRelation relation = sourceIssue.addRelation(targetIssue, relationType);

        WorkspaceMember actor = workspaceMemberFinder.getBy(sourceIssueIdentifier.workspaceKey(), memberId);
        eventPublisher.publishRelationAdded(sourceIssue, targetIssue, relation, actor);
    }

    @Override
    public void remove(IssueIdentifier sourceIssueIdentifier, String targetIssueKey, Long memberId) {

        Issue sourceIssue =
                issueFinder.getWithProjectBy(sourceIssueIdentifier.workspaceKey(), sourceIssueIdentifier.issueKey());
        Issue targetIssue = issueFinder.getWithProjectBy(sourceIssueIdentifier.workspaceKey(), targetIssueKey);

        projectAuthorizationService.requireProjectMember(targetIssue.getProject(), memberId);

        IssueRelation removedRelation = sourceIssue.removeRelation(targetIssue);

        WorkspaceMember actor = workspaceMemberFinder.getBy(sourceIssueIdentifier.workspaceKey(), memberId);
        eventPublisher.publishRelationRemoved(sourceIssue, targetIssue, removedRelation, actor);
    }
}
