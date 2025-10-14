package com.tissue.api.issue.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.AddIssueRelationCommand;
import com.tissue.api.issue.application.finder.IssueFinder;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.IssueRelation;
import com.tissue.api.issue.domain.service.RelationCycleDetector;
import com.tissue.api.issue.presentation.dto.response.IssueRelationResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueRelationService {

	private final IssueFinder issueFinder;
	private final RelationCycleDetector relationCycleDetector;

	@Transactional
	public IssueRelationResponse add(AddIssueRelationCommand cmd) {
		Issue source = issueFinder.findIssue(cmd.sourceIssueKey(), cmd.workspaceKey());
		Issue target = issueFinder.findIssue(cmd.targetIssueKey(), cmd.workspaceKey());

		if (cmd.relationType().requiresAcyclicCheck()) {
			relationCycleDetector.ensureNoCycle(source, target, cmd.relationType());
		}

		IssueRelation relation = source.addRelation(target, cmd.relationType());

		return IssueRelationResponse.from(source, target, relation);
	}

	@Transactional
	public void remove(
		String workspaceKey,
		String sourceIssueKey,
		String targetIssueKey
	) {
		Issue source = issueFinder.findIssue(sourceIssueKey, workspaceKey);
		Issue target = issueFinder.findIssue(targetIssueKey, workspaceKey);

		source.removeRelation(target);
	}
}
