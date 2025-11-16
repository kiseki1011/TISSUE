package com.tissue.api.issue.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.api.issue.application.dto.response.IssueRelationResult;
import com.tissue.api.issue.application.port.in.IssueRelationUseCase;
import com.tissue.api.issue.application.service.finder.IssueFinder;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueRelation;
import com.tissue.api.issue.domain.service.relation.RelationCycleDetector;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueRelationService implements IssueRelationUseCase {

	private final IssueFinder issueFinder;
	private final RelationCycleDetector relationCycleDetector;

	@Override
	public IssueRelationResult add(AddIssueRelationCommand cmd) {
		Issue source = issueFinder.findIssue(cmd.sourceIssueKey(), cmd.workspaceKey());
		Issue target = issueFinder.findIssue(cmd.targetIssueKey(), cmd.workspaceKey());

		if (cmd.relationType().requiresAcyclicCheck()) {
			relationCycleDetector.ensureNoCycle(source, target, cmd.relationType());
		}

		IssueRelation relation = source.addRelation(target, cmd.relationType());

		return IssueRelationResult.from(source, target, relation);
	}

	@Override
	public void remove(String workspaceKey, String sourceIssueKey, String targetIssueKey) {
		Issue source = issueFinder.findIssue(sourceIssueKey, workspaceKey);
		Issue target = issueFinder.findIssue(targetIssueKey, workspaceKey);

		source.removeRelation(target);
	}
}
