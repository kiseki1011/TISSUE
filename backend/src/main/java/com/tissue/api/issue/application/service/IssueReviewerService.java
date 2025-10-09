package com.tissue.api.issue.application.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.finder.IssueFinder;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.application.dto.AddReviewerCommand;
import com.tissue.api.issue.application.dto.RemoveReviewerCommand;
import com.tissue.api.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.api.issue.presentation.dto.response.IssueReviewerResponse;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberFinder;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueReviewerService {

	private final IssueFinder issueFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public IssueReviewerResponse addReviewer(
		String workspaceCode,
		String issueKey,
		AddReviewerCommand command,
		Long requesterMemberId
	) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceCode);

		WorkspaceMember reviewer = workspaceMemberFinder.findWorkspaceMember(
			command.memberId(),
			workspaceCode
		);

		WorkspaceMember requester = workspaceMemberFinder.findWorkspaceMember(
			requesterMemberId,
			workspaceCode
		);

		issue.addReviewer(reviewer);

		eventPublisher.publishEvent(
			IssueReviewerAddedEvent.createEvent(issue, requester, reviewer)
		);

		return IssueReviewerResponse.from(issue, reviewer);
	}

	@Transactional
	public IssueReviewerResponse removeReviewer(
		String workspaceCode,
		String issueKey,
		RemoveReviewerCommand command,
		Long requesterMemberId
	) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceCode);

		WorkspaceMember reviewer = workspaceMemberFinder.findWorkspaceMember(
			command.memberId(),
			workspaceCode
		);

		// WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(
		// 	requesterMemberId,
		// 	workspaceKey
		// );

		issue.removeReviewer(reviewer);

		return IssueReviewerResponse.from(issue, reviewer);
	}

	// TODO: IssueService 또는 IssueReviewService로 이동 고려
	// @Transactional
	// public IssueResponse requestReview(
	// 	String workspaceKey,
	// 	String issueKey,
	// 	Long requesterMemberId
	// ) {
	// 	Issue issue = issueReader.findIssue(issueKey, workspaceKey);
	//
	// 	// TODO: Authorization Service의 책임으로 옮기기?
	// 	// issue.validateIsAssignee(requesterMemberId);
	// 	issue.requestReview();
	//
	// 	eventPublisher.publishEvent(
	// 		IssueReviewRequestedEvent.createEvent(issue, requesterMemberId)
	// 	);
	//
	// 	return IssueResponse.from(issue);
	// }
}
