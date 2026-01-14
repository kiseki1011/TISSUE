package com.tissue.activitylog.application.listener;

import com.tissue.activitylog.application.dto.request.CreateLogCommand;
import com.tissue.activitylog.application.dto.request.CreateLogWithDiffCommand;
import com.tissue.activitylog.application.service.ActivityLogCommandService;
import com.tissue.activitylog.domain.enums.ActivityType;
import com.tissue.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.common.dto.FieldChange;
import com.tissue.common.vo.EntityReference;
import com.tissue.issue.domain.event.IssueAssignedEvent;
import com.tissue.issue.domain.event.IssueCreatedEvent;
import com.tissue.issue.domain.event.IssueDeletedEvent;
import com.tissue.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.issue.domain.event.IssueParentChangedEvent;
import com.tissue.issue.domain.event.IssueRelationAddedEvent;
import com.tissue.issue.domain.event.IssueRelationRemovedEvent;
import com.tissue.issue.domain.event.IssueReporterChangedEvent;
import com.tissue.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.issue.domain.event.IssueReviewerRemovedEvent;
import com.tissue.issue.domain.event.IssueStoryPointChangedEvent;
import com.tissue.issue.domain.event.IssueTransitionedEvent;
import com.tissue.issue.domain.event.IssueUnassignedEvent;
import com.tissue.sprint.domain.event.SprintCompletedEvent;
import com.tissue.sprint.domain.event.SprintStartedEvent;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ActivityLogEventListener {

    private final ActivityLogCommandService activityLogCommandService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueCreated(IssueCreatedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_CREATED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                List.of(event.projectKey(), event.issueKey(), event.actorDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueUpdated(IssueFieldsUpdatedEvent event) {
        CreateLogWithDiffCommand cmd = new CreateLogWithDiffCommand(
                event.eventId(),
                ActivityType.ISSUE_UPDATED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                List.of(event.issueKey(), event.actorDisplayName()),
                event.changes());

        activityLogCommandService.createLogWithDiff(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueCommentAdded(IssueCommentAddedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_COMMENT_ADDED,
                EntityReference.forIssueComment(
                        event.workspaceKey(), event.projectKey(), event.issueKey(), event.commentId()),
                event.actorMemberId(),
                List.of(event.issueKey(), event.actorDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueTransitioned(IssueTransitionedEvent event) {
        CreateLogWithDiffCommand cmd = new CreateLogWithDiffCommand(
                event.eventId(),
                ActivityType.ISSUE_STATUS_CHANGED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                List.of(event.issueKey(), event.actorDisplayName(), event.oldStatusName(), event.newStatusName()),
                Map.of("status", new FieldChange(event.oldStatusName(), event.newStatusName())));

        activityLogCommandService.createLogWithDiff(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueAssigned(IssueAssignedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_ASSIGNED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                List.of(event.issueKey(), event.actorDisplayName(), event.assigneeDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueUnassigned(IssueUnassignedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_UNASSIGNED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                List.of(event.issueKey(), event.actorDisplayName(), event.removedAssigneeDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueDeleted(IssueDeletedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_DELETED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                List.of(event.issueKey(), event.actorDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReporterChanged(IssueReporterChangedEvent event) {
        CreateLogWithDiffCommand cmd = new CreateLogWithDiffCommand(
                event.eventId(),
                ActivityType.ISSUE_REPORTER_CHANGED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                List.of(
                        event.issueKey(),
                        event.actorDisplayName(),
                        event.oldReporterDisplayName(),
                        event.newReporterDisplayName()),
                Map.of("reporter", new FieldChange(event.oldReporterDisplayName(), event.newReporterDisplayName())));

        activityLogCommandService.createLogWithDiff(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewerAdded(IssueReviewerAddedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_REVIEWER_ADDED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                List.of(event.issueKey(), event.actorDisplayName(), event.reviewerDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewerRemoved(IssueReviewerRemovedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_REVIEWER_REMOVED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                List.of(event.issueKey(), event.actorDisplayName(), event.removedReviewerDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewSubmitted(IssueReviewSubmittedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_REVIEW_SUBMITTED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                List.of(
                        event.issueKey(),
                        event.actorDisplayName(),
                        event.status().name()));

        activityLogCommandService.createLog(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStoryPointChanged(IssueStoryPointChangedEvent event) {
        CreateLogWithDiffCommand cmd = new CreateLogWithDiffCommand(
                event.eventId(),
                ActivityType.ISSUE_STORY_POINT_CHANGED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                List.of(
                        event.issueKey(),
                        event.actorDisplayName(),
                        String.valueOf(event.oldStoryPoint()),
                        String.valueOf(event.newStoryPoint())),
                Map.of("storyPoint", new FieldChange(event.oldStoryPoint(), event.newStoryPoint())));

        activityLogCommandService.createLogWithDiff(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleParentChanged(IssueParentChangedEvent event) {
        String oldParent = event.oldParentKey() != null ? event.oldParentKey() : "NONE";
        String newParent = event.newParentKey() != null ? event.newParentKey() : "NONE";

        CreateLogWithDiffCommand cmd = new CreateLogWithDiffCommand(
                event.eventId(),
                ActivityType.ISSUE_PARENT_CHANGED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                List.of(event.issueKey(), event.actorDisplayName(), oldParent, newParent),
                Map.of("parent", new FieldChange(oldParent, newParent)));

        activityLogCommandService.createLogWithDiff(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRelationAdded(IssueRelationAddedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_RELATION_ADDED,
                EntityReference.forIssue(
                        event.workspaceKey(), event.sourceProjectKey(), event.sourceIssueKey(), event.sourceIssueId()),
                event.actorMemberId(),
                List.of(
                        event.sourceIssueKey(),
                        event.actorDisplayName(),
                        event.relationType().name(),
                        event.targetIssueKey()));

        activityLogCommandService.createLog(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRelationRemoved(IssueRelationRemovedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_RELATION_REMOVED,
                EntityReference.forIssue(
                        event.workspaceKey(), event.sourceProjectKey(), event.sourceIssueKey(), event.sourceIssueId()),
                event.actorMemberId(),
                List.of(
                        event.sourceIssueKey(),
                        event.actorDisplayName(),
                        event.relationType().name(),
                        event.targetIssueKey()));

        activityLogCommandService.createLog(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSprintStarted(SprintStartedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.SPRINT_STARTED,
                EntityReference.forSprint(
                        event.workspaceKey(), event.projectKey(), event.sprintTitle(), event.sprintId()),
                event.actorMemberId(),
                List.of(event.projectKey(), event.sprintTitle(), event.actorDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSprintCompleted(SprintCompletedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.SPRINT_COMPLETED,
                EntityReference.forSprint(
                        event.workspaceKey(), event.projectKey(), event.sprintTitle(), event.sprintId()),
                event.actorMemberId(),
                List.of(event.projectKey(), event.sprintTitle(), event.actorDisplayName()));

        activityLogCommandService.createLog(cmd);
    }
}
