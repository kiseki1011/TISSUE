package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.port.in.IssueRelationUseCase;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueRelation;
import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.feature.issue.domain.service.relation.RelationCycleDetector;
import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueRelationService implements IssueRelationUseCase {

    private final IssueFinder issueFinder;
    private final RelationCycleDetector relationCycleDetector;
    private final IssueEventPublisher eventPublisher;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Override
    public void add(
            String sourceIssueKey,
            String targetProjectKey,
            String targetIssueKey,
            IssueRelationType relationType,
            ProjectMemberContext actorContext) {

        Issue sourceIssue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), sourceIssueKey);
        Issue targetIssue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), targetIssueKey);

        projectAuthorizationService.requireProjectMember(targetIssue.getProject(), actorContext.memberId());

        relationCycleDetector.ensureNoCycle(sourceIssue, targetIssue, relationType);
        IssueRelation relation = sourceIssue.addRelation(targetIssue, relationType);

        eventPublisher.publishRelationAdded(sourceIssue, targetIssue, relation, actorContext);
    }

    @Override
    public void remove(
            String sourceIssueKey, String targetProjectKey, String targetIssueKey, ProjectMemberContext actorContext) {

        Issue sourceIssue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), sourceIssueKey);
        Issue targetIssue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), targetIssueKey);

        projectAuthorizationService.requireProjectMember(targetIssue.getProject(), actorContext.memberId());

        IssueRelation removedRelation = sourceIssue.removeRelation(targetIssue);

        eventPublisher.publishRelationRemoved(sourceIssue, targetIssue, removedRelation, actorContext);
    }
}
