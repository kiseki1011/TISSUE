package com.tissue.api.issue.application.service;

import static com.tissue.api.common.util.IssueKeyUtil.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.api.issue.application.dto.request.RemoveIssueRelationCommand;
import com.tissue.api.issue.application.port.in.IssueRelationUseCase;
import com.tissue.api.issue.application.service.finder.IssueFinder;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueRelation;
import com.tissue.api.issue.domain.service.relation.RelationCycleDetector;
import com.tissue.api.project.application.service.finder.ProjectFinder;
import com.tissue.api.project.domain.Project;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueRelationService implements IssueRelationUseCase {

	private final ProjectFinder projectFinder;
	private final IssueFinder issueFinder;
	private final RelationCycleDetector relationCycleDetector;

	@Override
	@Transactional
	public void add(AddIssueRelationCommand cmd) {
		Project sourceProject = projectFinder.findForCommand(extractProjectKey(cmd.sourceIssueKey()),
			cmd.workspaceKey());
		Issue source = issueFinder.findBy(cmd.sourceIssueKey(), sourceProject);
		Project targetProject = projectFinder.findForCommand(extractProjectKey(cmd.targetIssueKey()),
			cmd.workspaceKey());
		Issue target = issueFinder.findBy(cmd.targetIssueKey(), targetProject);

		// TODO: relationCycleDetector.ensureNoCycle 내부로 if 로직 밀어넣기
		if (cmd.relationType().requiresAcyclicCheck()) {
			relationCycleDetector.ensureNoCycle(source, target, cmd.relationType());
		}

		IssueRelation relation = source.addRelation(target, cmd.relationType());

		// TODO: IssueRelationAddedEvent
	}

	@Override
	@Transactional
	public void remove(RemoveIssueRelationCommand cmd) {
		Project sourceProject = projectFinder.findForCommand(extractProjectKey(cmd.sourceIssueKey()),
			cmd.workspaceKey());
		Issue source = issueFinder.findBy(cmd.sourceIssueKey(), sourceProject);
		Project targetProject = projectFinder.findForCommand(extractProjectKey(cmd.targetIssueKey()),
			cmd.workspaceKey());
		Issue target = issueFinder.findBy(cmd.targetIssueKey(), targetProject);

		source.removeRelation(target);

		// TODO: IssueRelationRemovedEvent
	}
}
