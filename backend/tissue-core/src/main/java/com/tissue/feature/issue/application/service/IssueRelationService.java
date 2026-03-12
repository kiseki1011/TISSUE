package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.port.usecase.IssueRelationUseCase;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueRelation;
import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.feature.issue.domain.service.relation.RelationCycleDetector;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.dto.IssueIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueRelationService implements IssueRelationUseCase {

    private final IssueFinder issueFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final RelationCycleDetector relationCycleDetector;
    private final IssueEventPublisher eventPublisher;

    @Override
    public void add(
            IssueIdentifier sourceIid, String targetIssueKey, IssueRelationType relationType, Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(
                sourceIid.workspaceKey(), sourceIid.projectKey(), actorMemberId);

        Issue sourceIssue = issueFinder.getWithProjectBy(sourceIid.workspaceKey(), sourceIid.issueKey());
        Issue targetIssue = issueFinder.getWithProjectBy(sourceIid.workspaceKey(), targetIssueKey);

        relationCycleDetector.ensureNoCycle(sourceIssue, targetIssue, relationType);
        IssueRelation relation = sourceIssue.addRelation(targetIssue, relationType);

        eventPublisher.publishRelationAdded(sourceIssue, targetIssue, relation, actor);
    }

    @Override
    public void remove(IssueIdentifier sourceIid, String targetIssueKey, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(
                sourceIid.workspaceKey(), sourceIid.projectKey(), actorMemberId);

        Issue sourceIssue = issueFinder.getWithProjectBy(sourceIid.workspaceKey(), sourceIid.issueKey());
        Issue targetIssue = issueFinder.getWithProjectBy(sourceIid.workspaceKey(), targetIssueKey);

        IssueRelation removedRelation = sourceIssue.removeRelation(targetIssue);

        eventPublisher.publishRelationRemoved(sourceIssue, targetIssue, removedRelation, actor);
    }
}
