package com.tissue.issue.application.service;

import com.tissue.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.issue.application.dto.request.RemoveIssueRelationCommand;
import com.tissue.issue.application.port.in.IssueRelationUseCase;
import com.tissue.issue.application.service.authorization.IssueAuthorizationService;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueRelation;
import com.tissue.issue.domain.service.relation.RelationCycleDetector;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueRelationService implements IssueRelationUseCase {

    private final ProjectFinder projectFinder;
    private final IssueFinder issueFinder;
    private final RelationCycleDetector relationCycleDetector;
    private final IssueEventPublisher eventPublisher;
    private final IssueAuthorizationService issueAuthService;

    @Override
    public void add(AddIssueRelationCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        Project sourceProject = projectFinder.getModifiableBy(actorContext.projectId());
        Issue sourceIssue = issueFinder.getBy(cmd.sourceIssueKey(), sourceProject);

        issueAuthService.requireIssueEditPermission(sourceIssue, actorContext);

        Project targetProject = projectFinder.getModifiableBy(cmd.targetProjectKey(), actorContext.workspaceKey());
        Issue targetIssue = issueFinder.getBy(cmd.targetIssueKey(), targetProject);

        relationCycleDetector.ensureNoCycle(sourceIssue, targetIssue, cmd.relationType());
        IssueRelation relation = sourceIssue.addRelation(targetIssue, cmd.relationType());

        eventPublisher.publishRelationAdded(sourceIssue, targetIssue, relation, actorContext);
    }

    @Override
    public void remove(RemoveIssueRelationCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        Project sourceProject = projectFinder.getModifiableBy(actorContext.projectId());
        Issue sourceIssue = issueFinder.getBy(cmd.sourceIssueKey(), sourceProject);

        issueAuthService.requireIssueEditPermission(sourceIssue, actorContext);

        Project targetProject = projectFinder.getModifiableBy(cmd.targetProjectKey(), actorContext.workspaceKey());
        Issue targetIssue = issueFinder.getBy(cmd.targetIssueKey(), targetProject);

        IssueRelation removedRelation = sourceIssue.removeRelation(targetIssue);

        eventPublisher.publishRelationRemoved(sourceIssue, targetIssue, removedRelation, actorContext);
    }
}
