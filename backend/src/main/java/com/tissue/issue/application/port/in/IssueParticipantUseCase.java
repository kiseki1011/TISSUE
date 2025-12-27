package com.tissue.issue.application.port.in;

import static com.tissue.issue.application.service.authorization.IssueAuthExpressions.*;
import static com.tissue.project.application.service.authorization.ProjectAuthExpressions.*;

import com.tissue.issue.application.dto.request.AddReviewerCommand;
import com.tissue.issue.application.dto.request.AssignIssueCommand;
import com.tissue.issue.application.dto.request.ChangeReporterCommand;
import com.tissue.issue.application.dto.request.RemoveAssigneeCommand;
import com.tissue.issue.application.dto.request.RemoveReviewerCommand;
import com.tissue.issue.application.dto.request.SubscribeIssueCommand;
import com.tissue.issue.application.dto.request.UnsubscribeIssueCommand;
import org.springframework.security.access.prepost.PreAuthorize;

public interface IssueParticipantUseCase {

    @PreAuthorize(REQUIRES_ISSUE_PARTICIPANT_MANAGE_PERMISSION)
    void changeReporter(ChangeReporterCommand cmd);

    @PreAuthorize(REQUIRES_ISSUE_PARTICIPANT_MANAGE_PERMISSION)
    void assign(AssignIssueCommand cmd);

    @PreAuthorize(REQUIRES_ISSUE_PARTICIPANT_MANAGE_PERMISSION)
    void unassign(RemoveAssigneeCommand cmd);

    @PreAuthorize(REQUIRES_PROJECT_VIEWER)
    void subscribe(SubscribeIssueCommand cmd);

    @PreAuthorize(REQUIRES_PROJECT_VIEWER)
    void unsubscribe(UnsubscribeIssueCommand cmd);

    @PreAuthorize(REQUIRES_ISSUE_REVIEWER_MANAGE_PERMISSION)
    void addReviewer(AddReviewerCommand cmd);

    @PreAuthorize(REQUIRES_ISSUE_REVIEWER_MANAGE_PERMISSION)
    void removeReviewer(RemoveReviewerCommand cmd);
}
