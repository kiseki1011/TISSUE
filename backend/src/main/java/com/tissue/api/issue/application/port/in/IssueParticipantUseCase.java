package com.tissue.api.issue.application.port.in;

import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.request.AddReviewerCommand;
import com.tissue.api.issue.application.dto.request.AssignIssueCommand;
import com.tissue.api.issue.application.dto.request.ChangeReporterCommand;
import com.tissue.api.issue.application.dto.request.RemoveAssigneeCommand;
import com.tissue.api.issue.application.dto.request.RemoveReviewerCommand;
import com.tissue.api.issue.application.dto.request.SubscribeIssueCommand;
import com.tissue.api.issue.application.dto.request.UnsubscribeIssueCommand;
import com.tissue.api.issue.application.dto.response.IssueCommandResult;

@Transactional
public interface IssueParticipantUseCase {

	// @PreAuthorize(IssueSecurityExpressions.REQUIRES_AUTHOR + " OR " + ProjectSecurityExpressions.REQUIRES_ADMIN)
	IssueCommandResult changeReporter(ChangeReporterCommand cmd);

	// @PreAuthorize(IssueSecurityExpressions.REQUIRES_AUTHOR + " OR " + ProjectSecurityExpressions.REQUIRES_ADMIN)
	IssueCommandResult assign(AssignIssueCommand cmd);

	// @PreAuthorize(IssueSecurityExpressions.REQUIRES_AUTHOR + " OR " + ProjectSecurityExpressions.REQUIRES_ADMIN)
	IssueCommandResult unassign(RemoveAssigneeCommand cmd);

	// @PreAuthorize(ProjectSecurityExpressions.REQUIRES_VIEWER)
	IssueCommandResult subscribe(SubscribeIssueCommand cmd);

	// @PreAuthorize(ProjectSecurityExpressions.REQUIRES_SELF + " OR " + ProjectSecurityExpressions.REQUIRES_ADMIN)
	IssueCommandResult unsubscribe(UnsubscribeIssueCommand cmd);

	// @PreAuthorize(IssueSecurityExpressions.REQUIRES_AUTHOR + " OR " + ProjectSecurityExpressions.REQUIRES_ADMIN)
	IssueCommandResult addReviewer(AddReviewerCommand cmd);

	// @PreAuthorize(IssueSecurityExpressions.REQUIRES_AUTHOR + " OR " + ProjectSecurityExpressions.REQUIRES_ADMIN)
	// + 추가로 reviewer 본인이 본인을 제외하는 것도 가능. ProjectSecurityExpressions.REQUIRES_SELF 사용해야 하나?
	IssueCommandResult removeReviewer(RemoveReviewerCommand cmd);
}
