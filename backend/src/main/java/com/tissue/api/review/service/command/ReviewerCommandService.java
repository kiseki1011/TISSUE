package com.tissue.api.review.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.service.query.IssueQueryService;
import com.tissue.api.review.presentation.dto.response.AddReviewerResponse;
import com.tissue.api.review.presentation.dto.response.RemoveReviewerResponse;
import com.tissue.api.review.presentation.dto.response.RequestReviewResponse;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.service.query.WorkspaceMemberQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewerCommandService {

	private final IssueQueryService issueQueryService;
	private final WorkspaceMemberQueryService workspaceMemberQueryService;

	@Transactional
	public AddReviewerResponse addReviewer(
		String workspaceCode,
		String issueKey,
		Long reviewerWorkspaceMemberId,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = issueQueryService.findIssue(issueKey, workspaceCode);

		WorkspaceMember reviewer = workspaceMemberQueryService.findWorkspaceMember(
			reviewerWorkspaceMemberId,
			workspaceCode
		);
		WorkspaceMember requester = workspaceMemberQueryService.findWorkspaceMember(
			requesterWorkspaceMemberId,
			workspaceCode
		);

		reviewer.validateRoleIsHigherThanViewer();

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssignee(requesterWorkspaceMemberId);
		}

		issue.addReviewer(reviewer);

		return AddReviewerResponse.from(reviewer);
	}

	@Transactional
	public RemoveReviewerResponse removeReviewer(
		String workspaceCode,
		String issueKey,
		Long reviewerWorkspaceMemberId,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = issueQueryService.findIssue(issueKey, workspaceCode);

		WorkspaceMember reviewer = workspaceMemberQueryService.findWorkspaceMember(
			reviewerWorkspaceMemberId,
			workspaceCode
		);
		WorkspaceMember requester = workspaceMemberQueryService.findWorkspaceMember(
			requesterWorkspaceMemberId,
			workspaceCode
		);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateCanRemoveReviewer(requesterWorkspaceMemberId, reviewerWorkspaceMemberId);
		}

		issue.removeReviewer(reviewer);

		return RemoveReviewerResponse.from(reviewer, issue);
	}

	@Transactional
	public RequestReviewResponse requestReview(
		String workspaceCode,
		String issueKey,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = issueQueryService.findIssue(issueKey, workspaceCode);

		issue.validateIsAssignee(requesterWorkspaceMemberId);
		issue.requestReview();

		return RequestReviewResponse.from(issue);
	}
}
