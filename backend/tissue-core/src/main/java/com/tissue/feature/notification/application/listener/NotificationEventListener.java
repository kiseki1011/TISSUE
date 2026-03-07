package com.tissue.feature.notification.application.listener;

import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ACTOR_NAME;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.CHANGED_FIELDS;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.CONTENT;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ENDED_AT;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ISSUE_KEY;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.JOINED_MEMBER_NAME;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.NEW_ROLE;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.NEW_STATE;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.OLD_ROLE;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.OLD_STATE;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.PROJECT_KEY;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.REMOVED_REVIEWER_NAME;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ROLE;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.SPRINT_TITLE;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.STARTED_AT;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.STATUS;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.TARGET_NAME;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.WORKSPACE_KEY;

import com.tissue.feature.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.feature.comment.domain.event.IssueCommentUpdatedEvent;
import com.tissue.feature.issue.domain.event.IssueAssignedEvent;
import com.tissue.feature.issue.domain.event.IssueCreatedEvent;
import com.tissue.feature.issue.domain.event.IssueDeletedEvent;
import com.tissue.feature.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewRequestedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewerRemovedEvent;
import com.tissue.feature.issue.domain.event.IssueTransitionedBySystemEvent;
import com.tissue.feature.issue.domain.event.IssueTransitionedEvent;
import com.tissue.feature.issue.domain.event.IssueUnassignedEvent;
import com.tissue.feature.notification.application.service.NotificationCommandService;
import com.tissue.feature.notification.application.service.NotificationTargetService;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.sprint.domain.event.SprintCompletedEvent;
import com.tissue.feature.sprint.domain.event.SprintStartedEvent;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberContactInfo;
import com.tissue.feature.workspace.domain.event.MemberJoinedWorkspaceEvent;
import com.tissue.feature.workspace.domain.event.WorkspaceRoleChangedEvent;
import com.tissue.shared.vo.EntityReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationCommandService commandService;
    private final NotificationTargetService targetService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueCreated(IssueCreatedEvent event) {
        List<WorkspaceMemberContactInfo> targets = targetService.getProjectMembersExcluding(
                event.workspaceKey(), event.projectKey(), event.actorMemberId());

        log.info(
                "Handling IssueCreatedEvent: issue={} in project={}, targets={}",
                event.issueKey(),
                event.projectKey(),
                targets.size());

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_CREATED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY, event.workspaceKey(),
                        PROJECT_KEY, event.projectKey(),
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueAssigned(IssueAssignedEvent event) {
        Collection<WorkspaceMemberContactInfo> targets =
                targetService.getIssueAssignee(event.workspaceKey(), event.issueKey());

        removeReceiverFromTargets(targets, event.actorMemberId());

        log.info(
                "Handling IssueAssignedEvent: issue={}, assignee={}, targets={}",
                event.issueKey(),
                event.assigneeMemberId(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_ASSIGNED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY, event.workspaceKey(),
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueUnassigned(IssueUnassignedEvent event) {
        if (event.removedAssigneeMemberId().equals(event.actorMemberId())) {
            return;
        }

        Collection<WorkspaceMemberContactInfo> targets =
                targetService.getSpecificMemberTarget(event.workspaceKey(), event.removedAssigneeMemberId());

        log.info(
                "Handling IssueUnassignedEvent: issue={}, removedAssignee={}, targets={}",
                event.issueKey(),
                event.removedAssigneeMemberId(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_UNASSIGNED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY, event.workspaceKey(),
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueFieldsUpdated(IssueFieldsUpdatedEvent event) {
        Collection<WorkspaceMemberContactInfo> targets =
                targetService.getIssueParticipants(event.workspaceKey(), event.issueKey());

        removeReceiverFromTargets(targets, event.actorMemberId());

        String changedFields = String.join(", ", event.changes().keySet());

        log.info(
                "Handling IssueFieldsUpdatedEvent: issue={}, fields=[{}], targets={}",
                event.issueKey(),
                changedFields,
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_UPDATED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY, event.workspaceKey(),
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName(),
                        CHANGED_FIELDS, changedFields));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueTransitioned(IssueTransitionedEvent event) {
        Collection<WorkspaceMemberContactInfo> targets =
                targetService.getIssueParticipantsAndReviewers(event.workspaceKey(), event.issueKey());

        removeReceiverFromTargets(targets, event.actorMemberId());

        log.info(
                "Handling IssueTransitionedEvent: issue={}, {} -> {}, targets={}",
                event.issueKey(),
                event.oldStateName(),
                event.newStateName(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_STATUS_CHANGED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY, event.workspaceKey(),
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName(),
                        OLD_STATE, event.oldStateName(),
                        NEW_STATE, event.newStateName()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransitionedBySystem(IssueTransitionedBySystemEvent event) {
        Collection<WorkspaceMemberContactInfo> targets =
                targetService.getIssueParticipantsAndReviewers(event.workspaceKey(), event.issueKey());

        log.info(
                "Handling IssueTransitionedBySystemEvent: issue={}, {} -> {}, targets={}",
                event.issueKey(),
                event.oldStateName(),
                event.newStateName(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey());

        String vcsUser = event.vcsUserName() != null ? event.vcsUserName() : "System";
        String actorName = "System (" + vcsUser + ")";

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_STATUS_CHANGED,
                reference,
                targets,
                null,
                actorName,
                Map.of(
                        WORKSPACE_KEY, event.workspaceKey(),
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, actorName,
                        OLD_STATE, event.oldStateName(),
                        NEW_STATE, event.newStateName()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewerAdded(IssueReviewerAddedEvent event) {
        if (event.reviewerMemberId().equals(event.actorMemberId())) {
            return;
        }

        Collection<WorkspaceMemberContactInfo> targets =
                targetService.getSpecificMemberTarget(event.workspaceKey(), event.reviewerMemberId());

        log.info(
                "Handling IssueReviewerAddedEvent: issue={}, reviewer={}, targets={}",
                event.issueKey(),
                event.reviewerMemberId(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_REVIEWER_ADDED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY, event.workspaceKey(),
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewSubmitted(IssueReviewSubmittedEvent event) {
        Collection<WorkspaceMemberContactInfo> targets =
                targetService.getIssueAssigneeAndReporter(event.workspaceKey(), event.issueKey());

        removeReceiverFromTargets(targets, event.actorMemberId());

        log.info(
                "Handling IssueReviewSubmittedEvent: issue={}, status={}, targets={}",
                event.issueKey(),
                event.reviewStatus(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_REVIEW_SUBMITTED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY, event.workspaceKey(),
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName(),
                        STATUS, event.reviewStatus().name()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueDeleted(IssueDeletedEvent event) {
        Collection<WorkspaceMemberContactInfo> targets =
                targetService.getIssueParticipantsAndReviewers(event.workspaceKey(), event.issueKey());

        removeReceiverFromTargets(targets, event.actorMemberId());

        log.info("Handling IssueDeletedEvent: issue={}, targets={}", event.issueKey(), targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_DELETED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY, event.workspaceKey(),
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueCommentAdded(IssueCommentAddedEvent event) {
        List<WorkspaceMemberContactInfo> mentionedMembers =
                targetService.getMembersByUsernames(event.workspaceKey(), new HashSet<>(event.mentionedUsernames()));

        removeReceiverFromTargets(mentionedMembers, event.actorMemberId());

        Set<WorkspaceMemberContactInfo> participants =
                targetService.getIssueParticipantsAndReviewers(event.workspaceKey(), event.issueKey());

        removeReceiverFromTargets(participants, event.actorMemberId());
        removeMentionedMembersFromParticipants(mentionedMembers, participants);

        log.info(
                "Handling IssueCommentAddedEvent: issue={}, mentioned={}, participants={}",
                event.issueKey(),
                mentionedMembers.size(),
                participants.size());

        EntityReference reference = EntityReference.forIssueComment(
                event.workspaceKey(), event.projectKey(), event.issueKey(), event.commentId());

        if (!mentionedMembers.isEmpty()) {
            commandService.createAndSend(
                    event.eventId(),
                    NotificationType.ISSUE_MENTIONED,
                    reference,
                    mentionedMembers,
                    event.actorMemberId(),
                    event.actorDisplayName(),
                    Map.of(
                            WORKSPACE_KEY, event.workspaceKey(),
                            ISSUE_KEY, event.issueKey(),
                            ACTOR_NAME, event.actorDisplayName(),
                            CONTENT, event.content()));
        }

        if (!participants.isEmpty()) {
            commandService.createAndSend(
                    event.eventId(),
                    NotificationType.ISSUE_COMMENT_ADDED,
                    reference,
                    participants,
                    event.actorMemberId(),
                    event.actorDisplayName(),
                    Map.of(
                            WORKSPACE_KEY, event.workspaceKey(),
                            ISSUE_KEY, event.issueKey(),
                            ACTOR_NAME, event.actorDisplayName(),
                            CONTENT, event.content()));
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueCommentUpdated(IssueCommentUpdatedEvent event) {
        List<WorkspaceMemberContactInfo> mentionedMembers =
                targetService.getMembersByUsernames(event.workspaceKey(), new HashSet<>(event.mentionedUsernames()));

        removeReceiverFromTargets(mentionedMembers, event.actorMemberId());

        Set<WorkspaceMemberContactInfo> participants =
                targetService.getIssueParticipantsAndReviewers(event.workspaceKey(), event.issueKey());

        removeReceiverFromTargets(participants, event.actorMemberId());
        removeMentionedMembersFromParticipants(mentionedMembers, participants);

        log.info(
                "Handling IssueCommentUpdatedEvent: issue={}, mentioned={}, participants={}",
                event.issueKey(),
                mentionedMembers.size(),
                participants.size());

        EntityReference reference = EntityReference.forIssueComment(
                event.workspaceKey(), event.projectKey(), event.issueKey(), event.commentId());

        if (!mentionedMembers.isEmpty()) {
            commandService.createAndSend(
                    event.eventId(),
                    NotificationType.ISSUE_MENTIONED,
                    reference,
                    mentionedMembers,
                    event.actorMemberId(),
                    event.actorDisplayName(),
                    Map.of(
                            WORKSPACE_KEY, event.workspaceKey(),
                            ISSUE_KEY, event.issueKey(),
                            ACTOR_NAME, event.actorDisplayName(),
                            CONTENT, event.content()));
        }

        if (!participants.isEmpty()) {
            commandService.createAndSend(
                    event.eventId(),
                    NotificationType.ISSUE_COMMENT_UPDATED,
                    reference,
                    participants,
                    event.actorMemberId(),
                    event.actorDisplayName(),
                    Map.of(
                            WORKSPACE_KEY, event.workspaceKey(),
                            ISSUE_KEY, event.issueKey(),
                            ACTOR_NAME, event.actorDisplayName(),
                            CONTENT, event.content()));
        }
    }

    private void removeMentionedMembersFromParticipants(
            List<WorkspaceMemberContactInfo> mentionedMembers, Set<WorkspaceMemberContactInfo> participants) {

        Set<Long> mentionedMemberIds = mentionedMembers.stream()
                .map(WorkspaceMemberContactInfo::getMemberId)
                .collect(Collectors.toSet());
        participants.removeIf(p -> mentionedMemberIds.contains(p.getMemberId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewerRemoved(IssueReviewerRemovedEvent event) {
        if (event.removedReviewerMemberId().equals(event.actorMemberId())) {
            return;
        }

        Collection<WorkspaceMemberContactInfo> targets =
                targetService.getSpecificMemberTarget(event.workspaceKey(), event.removedReviewerMemberId());

        log.info(
                "Handling IssueReviewerRemovedEvent: issue={}, removedReviewer={}, targets={}",
                event.issueKey(),
                event.removedReviewerMemberId(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_REVIEWER_REMOVED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY, event.workspaceKey(),
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName(),
                        REMOVED_REVIEWER_NAME, event.removedReviewerDisplayName()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewRequested(IssueReviewRequestedEvent event) {
        Collection<WorkspaceMemberContactInfo> targets;

        if (event.reviewerMemberIds() != null && !event.reviewerMemberIds().isEmpty()) {
            targets = targetService.getSpecificMembersTargets(event.workspaceKey(), event.reviewerMemberIds());
        } else {
            targets = targetService.getIssueReviewers(event.workspaceKey(), event.issueKey());
        }

        removeReceiverFromTargets(targets, event.actorMemberId());

        log.info("Handling IssueReviewRequestedEvent: issue={}, targets={}", event.issueKey(), targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_REVIEW_REQUESTED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY, event.workspaceKey(),
                        ISSUE_KEY, event.issueKey(),
                        ACTOR_NAME, event.actorDisplayName()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSprintStarted(SprintStartedEvent event) {
        Collection<WorkspaceMemberContactInfo> targets =
                targetService.getAllProjectMembers(event.workspaceKey(), event.projectKey());

        removeReceiverFromTargets(targets, event.actorMemberId());

        log.info("Handling SprintStartedEvent: sprint={}, targets={}", event.sprintTitle(), targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forSprint(event.workspaceKey(), event.projectKey(), event.sprintId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.SPRINT_STARTED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY, event.workspaceKey(),
                        SPRINT_TITLE, event.sprintTitle()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSprintCompleted(SprintCompletedEvent event) {
        Collection<WorkspaceMemberContactInfo> targets =
                targetService.getAllProjectMembers(event.workspaceKey(), event.projectKey());

        removeReceiverFromTargets(targets, event.actorMemberId());

        log.info("Handling SprintCompletedEvent: sprint={}, targets={}", event.sprintTitle(), targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forSprint(event.workspaceKey(), event.projectKey(), event.sprintId());

        String startedAt = event.startedAt() != null ? event.startedAt().toString() : "";
        String endedAt = event.endedAt() != null ? event.endedAt().toString() : "";

        commandService.createAndSend(
                event.eventId(),
                NotificationType.SPRINT_COMPLETED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY,
                        event.workspaceKey(),
                        SPRINT_TITLE,
                        event.sprintTitle(),
                        STARTED_AT,
                        startedAt,
                        ENDED_AT,
                        endedAt));
    }

    // TODO: consider removing (or target only workspace admins)
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberJoinedWorkspace(MemberJoinedWorkspaceEvent event) {
        Collection<WorkspaceMemberContactInfo> targets = targetService.getWorkspaceAdmins(event.workspaceKey());

        // exclude if actor or joined member is an admin (to avoid self notification)
        targets.removeIf(t ->
                t.getMemberId().equals(event.actorMemberId()) || t.getMemberId().equals(event.joinedMemberId()));

        log.info(
                "Handling MemberJoinedWorkspaceEvent: member={}, workspace={}, targets={}",
                event.joinedMemberId(),
                event.workspaceKey(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forWorkspaceMember(event.workspaceKey(), event.joinedMemberId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.MEMBER_JOINED_WORKSPACE,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY, event.workspaceKey(),
                        JOINED_MEMBER_NAME, event.joinedMemberDisplayName(),
                        ROLE, event.role().name()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWorkspaceRoleChanged(WorkspaceRoleChangedEvent event) {
        if (event.targetMemberId().equals(event.actorMemberId())) {
            return;
        }

        Collection<WorkspaceMemberContactInfo> targets =
                targetService.getSpecificMemberTarget(event.workspaceKey(), event.targetMemberId());

        log.info(
                "Handling WorkspaceRoleChangedEvent: targetMember={}, newRole={}, targets={}",
                event.targetMemberId(),
                event.newRole(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forWorkspaceMember(event.workspaceKey(), event.targetMemberId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.WORKSPACE_ROLE_CHANGED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY,
                        event.workspaceKey(),
                        TARGET_NAME,
                        event.targetDisplayName(),
                        ACTOR_NAME,
                        event.actorDisplayName(),
                        OLD_ROLE,
                        event.oldRole().name(),
                        NEW_ROLE,
                        event.newRole().name()));
    }

    private void removeReceiverFromTargets(Collection<WorkspaceMemberContactInfo> targets, Long memberId) {
        if (targets == null || memberId == null) {
            return;
        }
        targets.removeIf(target -> target.getMemberId().equals(memberId));
    }
}
