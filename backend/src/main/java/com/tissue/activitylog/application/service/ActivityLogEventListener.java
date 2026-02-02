package com.tissue.activitylog.application.service;

import static com.tissue.activitylog.domain.ActivityLogDataKeys.ACTOR_NAME;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.ASSIGNEE_NAME;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.BRANCH_NAME;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.ISSUE_KEY;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.NEW_PARENT;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.NEW_POINT;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.NEW_STATE;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.OLD_PARENT;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.OLD_POINT;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.OLD_STATE;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.PARENT;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.PROJECT_KEY;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.PR_ACTION;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.PR_TITLE;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.PR_URL;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.RELATION_TYPE;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.REMOVED_ASSIGNEE_NAME;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.REMOVED_REVIEWER_NAME;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.REPO_URL;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.REVIEWER_COUNT;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.REVIEWER_NAME;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.REVIEW_STATUS;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.SOURCE_ISSUE_KEY;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.SPRINT_TITLE;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.STATE;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.STORY_POINT;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.TARGET_ISSUE_KEY;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.TRIGGER_REASON;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.VCS_PROVIDER;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.VCS_USER_EMAIL;
import static com.tissue.activitylog.domain.ActivityLogDataKeys.VCS_USER_NAME;

import com.tissue.activitylog.application.dto.request.CreateLogCommand;
import com.tissue.activitylog.application.dto.request.CreateLogWithDiffCommand;
import com.tissue.activitylog.domain.ActivityType;
import com.tissue.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.common.dto.FieldChange;
import com.tissue.global.vo.EntityReference;
import com.tissue.issue.domain.event.IssueAssignedEvent;
import com.tissue.issue.domain.event.IssueBranchLinkedEvent;
import com.tissue.issue.domain.event.IssueCreatedEvent;
import com.tissue.issue.domain.event.IssueDeletedEvent;
import com.tissue.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.issue.domain.event.IssueParentChangedEvent;
import com.tissue.issue.domain.event.IssueRelationAddedEvent;
import com.tissue.issue.domain.event.IssueRelationRemovedEvent;
import com.tissue.issue.domain.event.IssueReviewRequestedEvent;
import com.tissue.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.issue.domain.event.IssueReviewerRemovedEvent;
import com.tissue.issue.domain.event.IssueStoryPointChangedEvent;
import com.tissue.issue.domain.event.IssueTransitionedBySystemEvent;
import com.tissue.issue.domain.event.IssueTransitionedEvent;
import com.tissue.issue.domain.event.IssueUnassignedEvent;
import com.tissue.issue.domain.event.IssueVcsConnectionEvent;
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
                        PROJECT_KEY, event.projectKey(),
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName()));

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
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName()),
                event.changes());

        activityLogCommandService.createLogWithDiff(cmd);
    }

    @EventListener
    public void handleBranchLinked(IssueBranchLinkedEvent event) {
        if (event.actorMemberId() == null) {
            return;
        }
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_BRANCH_CONNECTED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName(),
                        BRANCH_NAME, event.branchName(),
                        REPO_URL, event.repoUrl()));

        activityLogCommandService.createLog(cmd);
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
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName()));

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
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName(),
                        OLD_STATE, event.oldStateName(),
                        NEW_STATE, event.newStateName()),
                Map.of(STATE, new FieldChange(event.oldStateName(), event.newStateName())));

        activityLogCommandService.createLogWithDiff(cmd);
    }

    @EventListener
    public void handleTransitionedBySystem(IssueTransitionedBySystemEvent event) {
        String vcsUser = event.vcsUserName() != null ? event.vcsUserName() : "username_not_found";
        String vcsEmail = event.vcsUserEmail() != null ? event.vcsUserEmail() : "email_not_found";
        String trigger = event.triggerReason() != null ? event.triggerReason() : "";

        CreateLogWithDiffCommand cmd = new CreateLogWithDiffCommand(
                event.eventId(),
                ActivityType.ISSUE_WORKFLOW_TRANSITIONED_BY_SYSTEM,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                null,
                Map.of(
                        ISSUE_KEY, event.issueKey(),
                        VCS_PROVIDER, event.vcsProvider().toString(),
                        VCS_USER_NAME, vcsUser,
                        VCS_USER_EMAIL, vcsEmail,
                        OLD_STATE, event.oldStateName(),
                        NEW_STATE, event.newStateName(),
                        TRIGGER_REASON, trigger),
                Map.of(STATE, new FieldChange(event.oldStateName(), event.newStateName())));

        activityLogCommandService.createLogWithDiff(cmd);
    }

    @EventListener
    public void handleVcsConnection(IssueVcsConnectionEvent event) {
        String actorName = event.actorDisplayName() != null ? event.actorDisplayName() : "System";
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_VCS_CONNECTION_LINKED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, actorName,
                        PR_TITLE, event.prTitle() != null ? event.prTitle() : "",
                        PR_URL, event.prUrl() != null ? event.prUrl() : "",
                        PR_ACTION, event.prAction().toString(),
                        VCS_USER_EMAIL, event.vcsUserEmail() != null ? event.vcsUserEmail() : "",
                        VCS_USER_NAME, event.vcsUserName() != null ? event.vcsUserName() : ""));

        activityLogCommandService.createLog(cmd);
    }

    @EventListener
    public void handleIssueAssigned(IssueAssignedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_ASSIGNED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName(),
                        ASSIGNEE_NAME, event.assigneeDisplayName()));

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
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName(),
                        REMOVED_ASSIGNEE_NAME, event.removedAssigneeDisplayName()));

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
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @EventListener
    public void handleIssueReviewerAdded(IssueReviewerAddedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.ISSUE_REVIEWER_ADDED,
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId()),
                event.actorMemberId(),
                Map.of(
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName(),
                        REVIEWER_NAME, event.reviewerDisplayName()));

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
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName(),
                        REMOVED_REVIEWER_NAME, event.removedReviewerDisplayName()));

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
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName(),
                        REVIEW_STATUS, event.reviewStatus().name()));

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
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName(),
                        REVIEWER_COUNT, String.valueOf(event.reviewerCount())));

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
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName(),
                        OLD_POINT, String.valueOf(event.oldStoryPoint()),
                        NEW_POINT, String.valueOf(event.newStoryPoint())),
                Map.of(STORY_POINT, new FieldChange(event.oldStoryPoint(), event.newStoryPoint())));

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
                        ISSUE_KEY,
                        event.issueKey(),
                        ACTOR_NAME,
                        event.actorDisplayName(),
                        OLD_PARENT,
                        oldParent,
                        NEW_PARENT,
                        newParent),
                Map.of(PARENT, new FieldChange(oldParent, newParent)));

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
                        SOURCE_ISSUE_KEY, event.sourceIssueKey(),
                        ACTOR_NAME, event.actorDisplayName(),
                        RELATION_TYPE, event.relationType().name(),
                        TARGET_ISSUE_KEY, event.targetIssueKey()));

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
                        SOURCE_ISSUE_KEY, event.sourceIssueKey(),
                        ACTOR_NAME, event.actorDisplayName(),
                        RELATION_TYPE, event.relationType().name(),
                        TARGET_ISSUE_KEY, event.targetIssueKey()));

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
                        PROJECT_KEY, event.projectKey(),
                        SPRINT_TITLE, event.sprintTitle(),
                        ACTOR_NAME, event.actorDisplayName()));

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
                        PROJECT_KEY, event.projectKey(),
                        SPRINT_TITLE, event.sprintTitle(),
                        ACTOR_NAME, event.actorDisplayName()));

        activityLogCommandService.createLog(cmd);
    }
}
