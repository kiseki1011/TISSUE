package com.tissue.issue.application.port.in;

import com.tissue.issue.application.dto.request.AddReviewerCommand;
import com.tissue.issue.application.dto.request.AssignIssueCommand;
import com.tissue.issue.application.dto.request.ChangeReporterCommand;
import com.tissue.issue.application.dto.request.RemoveAssigneeCommand;
import com.tissue.issue.application.dto.request.RemoveReviewerCommand;
import com.tissue.issue.application.dto.request.SubscribeIssueCommand;
import com.tissue.issue.application.dto.request.UnsubscribeIssueCommand;

public interface IssueParticipantUseCase {

    void changeReporter(ChangeReporterCommand cmd);

    void assign(AssignIssueCommand cmd);

    void unassign(RemoveAssigneeCommand cmd);

    void subscribe(SubscribeIssueCommand cmd);

    void unsubscribe(UnsubscribeIssueCommand cmd);

    void addReviewer(AddReviewerCommand cmd);

    void removeReviewer(RemoveReviewerCommand cmd);
}
