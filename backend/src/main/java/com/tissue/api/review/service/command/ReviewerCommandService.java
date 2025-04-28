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
import com.tissue.api.review.service.dto.AddReviewerCommand;
import com.tissue.api.review.service.dto.RemoveReviewerCommand;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
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
		AddReviewerCommand command,
		Long requesterMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		WorkspaceMember reviewer = workspaceMemberReader.findWorkspaceMember(
			command.memberId(),
			workspaceCode
		);

		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(
			requesterMemberId,
			workspaceCode
		);

		// TODO: Reviewer Authorization Service의 책임으로 옮기기?(서비스 호출)
		// reviewer.validateRoleIsHigherThanViewer();
		//
		// if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
		// 	issue.validateIsAssignee(requesterMemberId);
		// }

		issue.addReviewer(reviewer);

		eventPublisher.publishEvent(
			ReviewerAddedEvent.createEvent(issue, requester, reviewer)
		);

		return AddReviewerResponse.from(reviewer);
	}

	@Transactional
	public RemoveReviewerResponse removeReviewer(
		String workspaceCode,
		String issueKey,
		RemoveReviewerCommand command,
		Long requesterMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		WorkspaceMember reviewer = workspaceMemberReader.findWorkspaceMember(
			command.memberId(),
			workspaceCode
		);

		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(
			requesterMemberId,
			workspaceCode
		);

		// TODO: 굳이 검사할 필요가 있을까? 그냥 MEMBER 이상이면 해제할 수 있도록 해도 괜찮지 않을까?
		// TODO: Reviewer Authorization Service의 책임으로 옮기기?(서비스 호출)
		// if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
		// 	issue.validateCanRemoveReviewer(requesterMemberId, command.memberId());
		// }

		issue.removeReviewer(reviewer);

		return RemoveReviewerResponse.from(reviewer, issue);
	}

	@Transactional
	public RequestReviewResponse requestReview(
		String workspaceCode,
		String issueKey,
		Long requesterMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		// TODO: Authorization Service의 책임으로 옮기기?
		// issue.validateIsAssignee(requesterMemberId);
		issue.requestReview();

		eventPublisher.publishEvent(
			ReviewRequestedEvent.createEvent(issue, requesterMemberId)
		);

		return RequestReviewResponse.from(issue);
	}
}
