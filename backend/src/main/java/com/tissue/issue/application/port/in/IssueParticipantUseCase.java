package com.tissue.issue.application.port.in;

import static com.tissue.security.authorization.project.issue.IssueSecurityExpressions.*;
import static com.tissue.security.authorization.project.ProjectSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.issue.application.dto.request.AddReviewerCommand;
import com.tissue.issue.application.dto.request.AssignIssueCommand;
import com.tissue.issue.application.dto.request.ChangeReporterCommand;
import com.tissue.issue.application.dto.request.RemoveAssigneeCommand;
import com.tissue.issue.application.dto.request.RemoveReviewerCommand;
import com.tissue.issue.application.dto.request.SubscribeIssueCommand;
import com.tissue.issue.application.dto.request.UnsubscribeIssueCommand;

public interface IssueParticipantUseCase {

	@PreAuthorize(REQUIRES_ISSUE_PARTICIPANT_MANAGER)
	void changeReporter(ChangeReporterCommand cmd);

	@PreAuthorize(REQUIRES_ISSUE_PARTICIPANT_MANAGER)
	void assign(AssignIssueCommand cmd);

	@PreAuthorize(REQUIRES_ISSUE_PARTICIPANT_MANAGER)
	void unassign(RemoveAssigneeCommand cmd);

	// TODO: consider expanding the parameters instead of using a command dto(for REQUIRES_PROJECT_VIEWER spel)
	@PreAuthorize(REQUIRES_PROJECT_VIEWER)
	void subscribe(SubscribeIssueCommand cmd);

	// TODO: consider expanding the parameters instead of using a command dto(for REQUIRES_PROJECT_VIEWER spel)
	@PreAuthorize(REQUIRES_PROJECT_VIEWER)
	void unsubscribe(UnsubscribeIssueCommand cmd);

	@PreAuthorize(REQUIRES_ISSUE_REVIEWER_MANAGER)
	void addReviewer(AddReviewerCommand cmd);

	@PreAuthorize(REQUIRES_ISSUE_REVIEWER_MANAGER)
	void removeReviewer(RemoveReviewerCommand cmd);
}
