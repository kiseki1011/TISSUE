package com.tissue.activitylog.application.eventlistener;

import com.tissue.activitylog.application.dto.request.CreateLogCommand;
import com.tissue.activitylog.application.dto.request.CreateLogWithDiffCommand;
import com.tissue.activitylog.application.service.ActivityLogCommandService;
import com.tissue.activitylog.domain.ActivityType;
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
import com.tissue.issue.domain.event.IssueReviewRequestedEvent;
import com.tissue.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.issue.domain.event.IssueReviewerRemovedEvent;
import com.tissue.issue.domain.event.IssueStoryPointChangedEvent;
import com.tissue.issue.domain.event.IssueTransitionedEvent;
import com.tissue.issue.domain.event.IssueUnassignedEvent;
import com.tissue.sprint.domain.event.SprintCompletedEvent;
import com.tissue.sprint.domain.event.SprintStartedEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityLogEventListener {

    private final ActivityLogCommandService activityLogCommandService;

    @EventListener
    public void handleIssueCreated(IssueCreatedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_CREATED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        "projectKey", event.projectKey(),
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @EventListener
    public void handleIssueUpdated(IssueFieldsUpdatedEvent event) {
        CreateLogWithDiffCommand cmd = new CreateLogWithDiffCommand(
                event.eventId(),
                ActivityType.ISSUE_UPDATED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName()),
                event.changes());

        activityLogCommandService.createLogWithDiff(cmd);
    }

    @EventListener
    public void handleIssueCommentAdded(IssueCommentAddedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_COMMENT_ADDED,
                EntityReference.forIssueComment(
                        event.workspaceKey(), event.projectKey(), event.issueKey(), event.commentId()),
                event.actorMemberId(),
                Map.of(
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @EventListener
    public void handleIssueTransitioned(IssueTransitionedEvent event) {
        CreateLogWithDiffCommand cmd = new CreateLogWithDiffCommand(
                event.eventId(),
                ActivityType.ISSUE_WORKFLOW_TRANSITIONED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName(),
                        "oldState", event.oldStateName(),
                        "newState", event.newStateName()),
                Map.of("state", new FieldChange(event.oldStateName(), event.newStateName())));

        activityLogCommandService.createLogWithDiff(cmd);
    }

    @EventListener
    public void handleIssueAssigned(IssueAssignedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_ASSIGNED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName(),
                        "assigneeName", event.assigneeDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @EventListener
    public void handleIssueUnassigned(IssueUnassignedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_UNASSIGNED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName(),
                        "removedAssigneeName", event.removedAssigneeDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @EventListener
    public void handleIssueDeleted(IssueDeletedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_DELETED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @EventListener
    public void handleIssueReporterChanged(IssueReporterChangedEvent event) {
        CreateLogWithDiffCommand cmd = new CreateLogWithDiffCommand(
                event.eventId(),
                ActivityType.ISSUE_REPORTER_CHANGED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName(),
                        "oldReporter", event.oldReporterDisplayName(),
                        "newReporter", event.newReporterDisplayName()),
                Map.of("reporter", new FieldChange(event.oldReporterDisplayName(), event.newReporterDisplayName())));

        activityLogCommandService.createLogWithDiff(cmd);
    }

    @EventListener
    public void handleIssueReviewerAdded(IssueReviewerAddedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_REVIEWER_ADDED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName(),
                        "reviewerName", event.reviewerDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @EventListener
    public void handleIssueReviewerRemoved(IssueReviewerRemovedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_REVIEWER_REMOVED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName(),
                        "removedReviewerName", event.removedReviewerDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @EventListener
    public void handleIssueReviewSubmitted(IssueReviewSubmittedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_REVIEW_SUBMITTED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName(),
                        "reviewStatus", event.reviewStatus().name()));

        activityLogCommandService.createLog(cmd);
    }

    @EventListener
    public void handleIssueReviewRequested(IssueReviewRequestedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_REVIEW_REQUESTED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName(),
                        "reviewerCount", String.valueOf(event.reviewerCount())));

        activityLogCommandService.createLog(cmd);
    }

    @EventListener
    public void handleStoryPointChanged(IssueStoryPointChangedEvent event) {
        CreateLogWithDiffCommand cmd = new CreateLogWithDiffCommand(
                event.eventId(),
                ActivityType.ISSUE_STORY_POINT_CHANGED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName(),
                        "oldPoint", String.valueOf(event.oldStoryPoint()),
                        "newPoint", String.valueOf(event.newStoryPoint())),
                Map.of("storyPoint", new FieldChange(event.oldStoryPoint(), event.newStoryPoint())));

        activityLogCommandService.createLogWithDiff(cmd);
    }

    @EventListener
    public void handleParentChanged(IssueParentChangedEvent event) {
        String oldParent = event.oldParentKey() != null ? event.oldParentKey() : "NONE";
        String newParent = event.newParentKey() != null ? event.newParentKey() : "NONE";

        CreateLogWithDiffCommand cmd = new CreateLogWithDiffCommand(
                event.eventId(),
                ActivityType.ISSUE_PARENT_CHANGED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        "issueKey",
                        event.issueKey(),
                        "actorName",
                        event.actorDisplayName(),
                        "oldParent",
                        oldParent,
                        "newParent",
                        newParent),
                Map.of("parent", new FieldChange(oldParent, newParent)));

        activityLogCommandService.createLogWithDiff(cmd);
    }

    @EventListener
    public void handleRelationAdded(IssueRelationAddedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_RELATION_ADDED,
                EntityReference.forIssue(
                        event.workspaceKey(), event.sourceProjectKey(), event.sourceIssueKey(), event.sourceIssueId()),
                event.actorMemberId(),
                Map.of(
                        "sourceIssueKey", event.sourceIssueKey(),
                        "actorName", event.actorDisplayName(),
                        "relationType", event.relationType().name(),
                        "targetIssueKey", event.targetIssueKey()));

        activityLogCommandService.createLog(cmd);
    }

    @EventListener
    public void handleRelationRemoved(IssueRelationRemovedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_RELATION_REMOVED,
                EntityReference.forIssue(
                        event.workspaceKey(), event.sourceProjectKey(), event.sourceIssueKey(), event.sourceIssueId()),
                event.actorMemberId(),
                Map.of(
                        "sourceIssueKey", event.sourceIssueKey(),
                        "actorName", event.actorDisplayName(),
                        "relationType", event.relationType().name(),
                        "targetIssueKey", event.targetIssueKey()));

        activityLogCommandService.createLog(cmd);
    }

    @EventListener
    public void handleSprintStarted(SprintStartedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.SPRINT_STARTED,
                EntityReference.forSprint(event.workspaceKey(), event.projectKey(), event.sprintId()),
                event.actorMemberId(),
                Map.of(
                        "projectKey", event.projectKey(),
                        "sprintTitle", event.sprintTitle(),
                        "actorName", event.actorDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @EventListener
    public void handleSprintCompleted(SprintCompletedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.SPRINT_COMPLETED,
                EntityReference.forSprint(event.workspaceKey(), event.projectKey(), event.sprintId()),
                event.actorMemberId(),
                Map.of(
                        "projectKey", event.projectKey(),
                        "sprintTitle", event.sprintTitle(),
                        "actorName", event.actorDisplayName()));

        activityLogCommandService.createLog(cmd);
    }
}
