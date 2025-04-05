package com.tissue.api.review.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.service.command.IssueReader;
import com.tissue.api.review.domain.event.ReviewRequestedEvent;
import com.tissue.api.review.domain.event.ReviewerAddedEvent;
import com.tissue.api.review.presentation.dto.response.AddReviewerResponse;
import com.tissue.api.review.presentation.dto.response.RemoveReviewerResponse;
import com.tissue.api.review.presentation.dto.response.RequestReviewResponse;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewerCommandService {

	private final IssueReader issueReader;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public AddReviewerResponse addReviewer(
		String workspaceCode,
		String issueKey,
		Long reviewerWorkspaceMemberId,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		WorkspaceMember reviewer = workspaceMemberReader.findWorkspaceMember(
			reviewerWorkspaceMemberId,
			workspaceCode
		);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(
			requesterWorkspaceMemberId,
			workspaceCode
		);

		reviewer.validateRoleIsHigherThanViewer();

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssignee(requesterWorkspaceMemberId);
		}

		issue.addReviewer(reviewer);

		// TODO: ReviewerAddedEvent(대상: 이슈의 구독자 -> 이슈의 assignees, reviewers, watchers)
		eventPublisher.publishEvent(
			ReviewerAddedEvent.createEvent(issue, requesterWorkspaceMemberId, reviewerWorkspaceMemberId)
		);

		return AddReviewerResponse.from(reviewer);
	}

	@Transactional
	public RemoveReviewerResponse removeReviewer(
		String workspaceCode,
		String issueKey,
		Long reviewerWorkspaceMemberId,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		WorkspaceMember reviewer = workspaceMemberReader.findWorkspaceMember(
			reviewerWorkspaceMemberId,
			workspaceCode
		);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(
			requesterWorkspaceMemberId,
			workspaceCode
		);

		// TODO: 굳이 검사할 필요가 있을까? 그냥 MEMBER 이상이면 해제할 수 있도록 해도 괜찮지 않을까?
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
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		issue.validateIsAssignee(requesterWorkspaceMemberId);
		issue.requestReview();

		// TODO: ReviewRequestedEvent(대상: 리뷰어)
		eventPublisher.publishEvent(
			ReviewRequestedEvent.createEvent(issue, requesterWorkspaceMemberId)
		);

		return RequestReviewResponse.from(issue);
	}
}
