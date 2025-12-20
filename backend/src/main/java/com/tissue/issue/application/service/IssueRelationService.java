package com.tissue.issue.application.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.issue.application.dto.request.RemoveIssueRelationCommand;
import com.tissue.issue.application.port.in.IssueRelationUseCase;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueRelation;
import com.tissue.issue.domain.event.IssueRelationAddedEvent;
import com.tissue.issue.domain.event.IssueRelationRemovedEvent;
import com.tissue.issue.domain.service.relation.RelationCycleDetector;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueRelationService implements IssueRelationUseCase {

	private final ProjectFinder projectFinder;
	private final ProjectMemberFinder projectMemberFinder;
	private final IssueFinder issueFinder;
	private final RelationCycleDetector relationCycleDetector;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional
	public void add(AddIssueRelationCommand cmd) {
		Project sourceProject = projectFinder.getModifiableBy(cmd.sourceProjectKey(), cmd.workspaceKey());
		Issue source = issueFinder.findBy(cmd.sourceIssueKey(), sourceProject);
		Project targetProject = projectFinder.getModifiableBy(cmd.targetProjectKey(), cmd.workspaceKey());
		Issue target = issueFinder.findBy(cmd.targetIssueKey(), targetProject);

		ProjectMember actor = projectMemberFinder.findBy(sourceProject, cmd.actorMemberId());

		relationCycleDetector.ensureNoCycle(source, target, cmd.relationType());

		IssueRelation relation = source.addRelation(target, cmd.relationType());

		eventPublisher.publishEvent(IssueRelationAddedEvent.create(
			source,
			target,
			relation,
			actor
		));
	}

	@Override
	@Transactional
	public void remove(RemoveIssueRelationCommand cmd) {
		Project sourceProject = projectFinder.getModifiableBy(cmd.sourceProjectKey(), cmd.workspaceKey());
		Issue source = issueFinder.findBy(cmd.sourceIssueKey(), sourceProject);
		Project targetProject = projectFinder.getModifiableBy(cmd.targetProjectKey(), cmd.workspaceKey());
		Issue target = issueFinder.findBy(cmd.targetIssueKey(), targetProject);

		ProjectMember actor = projectMemberFinder.findBy(sourceProject, cmd.actorMemberId());

		IssueRelation removedRelation = source.removeRelation(target);

		eventPublisher.publishEvent(IssueRelationRemovedEvent.create(
			source,
			target,
			removedRelation,
			actor
		));
	}
}
