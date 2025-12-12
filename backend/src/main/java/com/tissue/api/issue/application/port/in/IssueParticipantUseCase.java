package com.tissue.api.issue.application.port.in;

import static com.tissue.api.security.authorization.ProjectSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.api.issue.application.dto.request.AddReviewerCommand;
import com.tissue.api.issue.application.dto.request.AssignIssueCommand;
import com.tissue.api.issue.application.dto.request.ChangeReporterCommand;
import com.tissue.api.issue.application.dto.request.RemoveAssigneeCommand;
import com.tissue.api.issue.application.dto.request.RemoveReviewerCommand;
import com.tissue.api.issue.application.dto.request.SubscribeIssueCommand;
import com.tissue.api.issue.application.dto.request.UnsubscribeIssueCommand;

public interface IssueParticipantUseCase {

	// @PreAuthorize(IssueSecurityExpressions.REQUIRES_AUTHOR + OR + REQUIRES_PROJECT_ADMIN)
	void changeReporter(ChangeReporterCommand cmd);

	// @PreAuthorize(IssueSecurityExpressions.REQUIRES_AUTHOR + " OR " + REQUIRES_PROJECT_ADMIN)
	void assign(AssignIssueCommand cmd);

	// @PreAuthorize(IssueSecurityExpressions.REQUIRES_AUTHOR + " OR " + REQUIRES_PROJECT_ADMIN)
	void unassign(RemoveAssigneeCommand cmd);

	@PreAuthorize(REQUIRES_PROJECT_VIEWER)
	void subscribe(SubscribeIssueCommand cmd);

	// TODO: 구독 해지는 본인만 가능하도록 하는게 좋겠지?
	// @PreAuthorize(REQUIRES_PROJECT_VIEWER + CommonSecurityExpressions.REQUIRES_SELF)
	void unsubscribe(UnsubscribeIssueCommand cmd);

	// @PreAuthorize(IssueSecurityExpressions.REQUIRES_AUTHOR + " OR " + REQUIRES_PROJECT_ADMIN)
	void addReviewer(AddReviewerCommand cmd);

	// @PreAuthorize(IssueSecurityExpressions.REQUIRES_AUTHOR + " OR " + REQUIRES_PROJECT_ADMIN)
	// TODO: 추가로 reviewer 본인이 본인을 제외하는 것도 가능. ProjectSecurityExpressions.REQUIRES_SELF 사용해야 하나?
	void removeReviewer(RemoveReviewerCommand cmd);
}
