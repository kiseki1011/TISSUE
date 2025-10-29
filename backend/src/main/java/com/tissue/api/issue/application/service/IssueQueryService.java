package com.tissue.api.issue.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.response.IssueCommonFieldsDetail;
import com.tissue.api.issue.application.dto.response.TransitionDetail;
import com.tissue.api.issue.application.port.in.IssueQueryUseCase;
import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.workflow.domain.model.Workflow;
import com.tissue.api.workspacemember.application.finder.WorkspaceMemberQueryFinder;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueQueryService implements IssueQueryUseCase {

	private final IssueQueryRepository issueQueryRepo;
	private final WorkspaceMemberQueryFinder wmFinder;

	public IssueCommonFieldsDetail getIssueDetails(String workspaceKey, String issueKey) {
		Issue issue = issueQueryRepo.findWithDetail(workspaceKey, issueKey)
			.orElseThrow(() -> new RuntimeException("Issue not found"));

		WorkspaceMember author = wmFinder.findIncludingArchived(issue.getCreatedBy(), workspaceKey);
		WorkspaceMember updatedBy = wmFinder.findIncludingArchived(issue.getLastModifiedBy(), workspaceKey);

		return IssueCommonFieldsDetail.from(issue, author, updatedBy);
	}

	public List<TransitionDetail> getAvailableTransitions(String workspaceKey, String issueKey) {
		Issue issue = issueQueryRepo.findWithBasicInfo(issueKey, workspaceKey)
			.orElseThrow(() -> new RuntimeException("Issue not found"));

		Workflow workflow = issue.getIssueType().getWorkflow();

		return workflow.getTransitions().stream()
			.filter(t -> t.getSourceState().equals(issue.getCurrentState()))
			.map(TransitionDetail::from)
			.toList();
	}
}
