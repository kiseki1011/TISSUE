package com.tissue.api.issue.application.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.AddReviewerCommand;
import com.tissue.api.issue.application.dto.RemoveReviewerCommand;
import com.tissue.api.issue.application.service.reader.IssueReader;
import com.tissue.api.issue.domain.event.IssueReviewRequestedEvent;
import com.tissue.api.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.presentation.controller.dto.response.IssueResponse;
import com.tissue.api.issue.presentation.controller.dto.response.IssueReviewerResponse;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueReviewerCommandService {

	private final IssueReader issueReader;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public IssueReviewerResponse addReviewer(
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

		return IssueReviewerResponse.from(issue, reviewer);
	}

	// TODO: IssueService로 이동하는게 맞지 않을까?
	@Transactional
	public IssueResponse requestReview(
		String workspaceCode,
		String issueKey,
		Long requesterMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		// TODO: Authorization Service의 책임으로 옮기기?
		// issue.validateIsAssignee(requesterMemberId);
		issue.requestReview();

		eventPublisher.publishEvent(
			IssueReviewRequestedEvent.createEvent(issue, requesterMemberId)
		);

		return IssueResponse.from(issue);
	}
}
