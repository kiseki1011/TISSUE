package com.tissue.notification.application.listener;

import com.tissue.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.common.vo.EntityReference;
import com.tissue.issue.domain.event.IssueAssignedEvent;
import com.tissue.issue.domain.event.IssueCreatedEvent;
import com.tissue.issue.domain.event.IssueDeletedEvent;
import com.tissue.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.issue.domain.event.IssueReporterChangedEvent;
import com.tissue.issue.domain.event.IssueReviewRequestedEvent;
import com.tissue.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.issue.domain.event.IssueReviewerRemovedEvent;
import com.tissue.issue.domain.event.IssueTransitionedEvent;
import com.tissue.issue.domain.event.IssueUnassignedEvent;
import com.tissue.notification.application.service.NotificationCommandService;
import com.tissue.notification.application.service.NotificationTargetService;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.project.domain.event.MemberJoinedProjectEvent;
import com.tissue.project.domain.event.ProjectRoleChangedEvent;
import com.tissue.sprint.domain.event.SprintCompletedEvent;
import com.tissue.sprint.domain.event.SprintStartedEvent;
import com.tissue.workspace.application.port.out.WorkspaceMemberContact;
import com.tissue.workspace.domain.event.MemberJoinedWorkspaceEvent;
import com.tissue.workspace.domain.event.WorkspaceRoleChangedEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    /**
     * Target: Notify all project members (exclude actor)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueCreated(IssueCreatedEvent event) {
        List<WorkspaceMemberContact> targets = targetService.getProjectMembersExcluding(
                event.workspaceKey(), event.projectKey(), event.actorMemberId());

        log.info(
                "[NOTIFICATION] Handling IssueCreatedEvent: issue={} in project={}, targets={}",
                event.issueKey(),
                event.projectKey(),
                targets.size());

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_CREATED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        "workspaceKey", event.workspaceKey(),
                        "projectKey", event.projectKey(),
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName()));
    }

    /**
     * Target: Notify the assignee (exclude actor)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueAssigned(IssueAssignedEvent event) {
        Collection<WorkspaceMemberContact> targets =
                targetService.getIssueAssignee(event.workspaceKey(), event.issueKey());

        targets.removeIf(t -> t.memberId().equals(event.actorMemberId()));

        log.info(
                "[NOTIFICATION] Handling IssueAssignedEvent: issue={}, assignee={}, targets={}",
                event.issueKey(),
                event.assigneeMemberId(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_ASSIGNED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        "workspaceKey", event.workspaceKey(),
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName()));
    }

    /**
     * Target: Notify the removed assignee (exclude actor)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueUnassigned(IssueUnassignedEvent event) {
        if (event.removedAssigneeMemberId().equals(event.actorMemberId())) {
            return;
        }

        Collection<WorkspaceMemberContact> targets =
                targetService.getSpecificMemberTarget(event.workspaceKey(), event.removedAssigneeMemberId());

        log.info(
                "[NOTIFICATION] Handling IssueUnassignedEvent: issue={}, removedAssignee={}, targets={}",
                event.issueKey(),
                event.removedAssigneeMemberId(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_UNASSIGNED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        "workspaceKey", event.workspaceKey(),
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName()));
    }

    /**
     * Target: Notify author, assignee, reporter, subscribers (exclude actor)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueFieldsUpdated(IssueFieldsUpdatedEvent event) {
        Collection<WorkspaceMemberContact> targets =
                targetService.getIssueParticipants(event.workspaceKey(), event.issueKey());

        targets.removeIf(t -> t.memberId().equals(event.actorMemberId()));

        String changedFields = String.join(", ", event.changes().keySet());

        log.info(
                "[NOTIFICATION] Handling IssueFieldsUpdatedEvent: issue={}, fields=[{}], targets={}",
                event.issueKey(),
                changedFields,
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_UPDATED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        "workspaceKey", event.workspaceKey(),
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName(),
                        "changedFields", changedFields));
    }

    /**
     * Target: Notify author, assignee, reporter, reviewers, subscribers (exclude actor)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueTransitioned(IssueTransitionedEvent event) {
        Collection<WorkspaceMemberContact> targets =
                targetService.getIssueParticipantsAndReviewers(event.workspaceKey(), event.issueKey());

        targets.removeIf(t -> t.memberId().equals(event.actorMemberId()));

        log.info(
                "[NOTIFICATION] Handling IssueTransitionedEvent: issue={}, {} -> {}, targets={}",
                event.issueKey(),
                event.oldStatusName(),
                event.newStatusName(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_STATUS_CHANGED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        "workspaceKey", event.workspaceKey(),
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName(),
                        "oldStatus", event.oldStatusName(),
                        "newStatus", event.newStatusName()));
    }

    /**
     * Target: Notify the new reporter (exclude actor)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReporterChanged(IssueReporterChangedEvent event) {
        Collection<WorkspaceMemberContact> targets =
                targetService.getIssueReporter(event.workspaceKey(), event.issueKey());

        targets.removeIf(t -> t.memberId().equals(event.actorMemberId()));

        log.info(
                "[NOTIFICATION] Handling IssueReporterChangedEvent: issue={}, targets={}",
                event.issueKey(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_REPORTER_CHANGED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        "workspaceKey", event.workspaceKey(),
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName()));
    }

    /**
     * Target: Notify the added reviewer (exclude actor)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewerAdded(IssueReviewerAddedEvent event) {
        if (event.reviewerMemberId().equals(event.actorMemberId())) {
            return;
        }

        Collection<WorkspaceMemberContact> targets =
                targetService.getSpecificMemberTarget(event.workspaceKey(), event.reviewerMemberId());

        log.info(
                "[NOTIFICATION] Handling IssueReviewerAddedEvent: issue={}, reviewer={}, targets={}",
                event.issueKey(),
                event.reviewerMemberId(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_REVIEWER_ADDED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        "workspaceKey", event.workspaceKey(),
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewSubmitted(IssueReviewSubmittedEvent event) {
        Collection<WorkspaceMemberContact> targets =
                targetService.getIssueAssigneeAndReporter(event.workspaceKey(), event.issueKey());

        targets.removeIf(t -> t.memberId().equals(event.actorMemberId()));

        log.info(
                "[NOTIFICATION] Handling IssueReviewSubmittedEvent: issue={}, status={}, targets={}",
                event.issueKey(),
                event.status(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_REVIEW_SUBMITTED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        "workspaceKey", event.workspaceKey(),
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName(),
                        "status", event.status().name()));
    }

    /**
     * Target: Notify author, assignee, reporter, reviewers, subscribers (exclude actor)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueDeleted(IssueDeletedEvent event) {
        Collection<WorkspaceMemberContact> targets =
                targetService.getIssueParticipantsAndReviewers(event.workspaceKey(), event.issueKey());

        targets.removeIf(t -> t.memberId().equals(event.actorMemberId()));

        log.info("[NOTIFICATION] Handling IssueDeletedEvent: issue={}, targets={}", event.issueKey(), targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_DELETED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        "workspaceKey", event.workspaceKey(),
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName()));
    }

    /**
     * Target: Notify issue participants (exclude actor)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueCommentAdded(IssueCommentAddedEvent event) {
        Collection<WorkspaceMemberContact> targets =
                targetService.getIssueParticipantsAndReviewers(event.workspaceKey(), event.issueKey());

        targets.removeIf(t -> t.memberId().equals(event.actorMemberId()));

        log.info(
                "[NOTIFICATION] Handling IssueCommentAddedEvent: issue={}, targets={}",
                event.issueKey(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forIssueComment(
                event.workspaceKey(), event.projectKey(), event.issueKey(), event.commentId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_COMMENT_ADDED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        "workspaceKey", event.workspaceKey(),
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName()));
    }

    /**
     * Target: Notify the removed reviewer (exclude actor)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewerRemoved(IssueReviewerRemovedEvent event) {
        if (event.removedReviewerMemberId().equals(event.actorMemberId())) {
            return;
        }

        Collection<WorkspaceMemberContact> targets =
                targetService.getSpecificMemberTarget(event.workspaceKey(), event.removedReviewerMemberId());

        log.info(
                "[NOTIFICATION] Handling IssueReviewerRemovedEvent: issue={}, removedReviewer={}, targets={}",
                event.issueKey(),
                event.removedReviewerMemberId(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_REVIEWER_REMOVED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        "workspaceKey", event.workspaceKey(),
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName(),
                        "removedReviewerName", event.removedReviewerDisplayName()));
    }

    /**
     * Target: Notify specific reviewers or all reviewers (exclude actor)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewRequested(IssueReviewRequestedEvent event) {
        Collection<WorkspaceMemberContact> targets;

        if (event.reviewerMemberIds() != null && !event.reviewerMemberIds().isEmpty()) {
            targets = targetService.getSpecificMembersTargets(event.workspaceKey(), event.reviewerMemberIds());
        } else {
            targets = targetService.getIssueReviewers(event.workspaceKey(), event.issueKey());
        }

        targets.removeIf(t -> t.memberId().equals(event.actorMemberId()));

        log.info(
                "[NOTIFICATION] Handling IssueReviewRequestedEvent: issue={}, targets={}",
                event.issueKey(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_REVIEW_REQUESTED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        "workspaceKey", event.workspaceKey(),
                        "issueKey", event.issueKey(),
                        "actorName", event.actorDisplayName()));
    }

    /**
     * Target: Notify project members
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSprintStarted(SprintStartedEvent event) {
        Collection<WorkspaceMemberContact> targets =
                targetService.getAllProjectMembers(event.workspaceKey(), event.projectKey());

        targets.removeIf(t -> t.memberId().equals(event.actorMemberId()));

        log.info(
                "[NOTIFICATION] Handling SprintStartedEvent: sprint={}, targets={}",
                event.sprintTitle(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forSprint(
                event.workspaceKey(), event.projectKey(), event.sprintTitle(), event.sprintId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.SPRINT_STARTED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        "workspaceKey", event.workspaceKey(),
                        "sprintName", event.sprintTitle()));
    }

    /**
     * Target: Notify project members
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSprintCompleted(SprintCompletedEvent event) {
        Collection<WorkspaceMemberContact> targets =
                targetService.getAllProjectMembers(event.workspaceKey(), event.projectKey());

        targets.removeIf(t -> t.memberId().equals(event.actorMemberId()));

        log.info(
                "[NOTIFICATION] Handling SprintCompletedEvent: sprint={}, targets={}",
                event.sprintTitle(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forSprint(
                event.workspaceKey(), event.projectKey(), event.sprintTitle(), event.sprintId());

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
                        "workspaceKey",
                        event.workspaceKey(),
                        "sprintName",
                        event.sprintTitle(),
                        "startedAt",
                        startedAt,
                        "endedAt",
                        endedAt));
    }

    /**
     * Target: Notify workspace admins (exclude actor)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberJoinedWorkspace(MemberJoinedWorkspaceEvent event) {
        Collection<WorkspaceMemberContact> targets = targetService.getWorkspaceAdmins(event.workspaceKey());

        // Exclude if actor or joined member is an admin (to avoid self notification)
        targets.removeIf(
                t -> t.memberId().equals(event.actorMemberId()) || t.memberId().equals(event.joinedMemberId()));

        log.info(
                "[NOTIFICATION] Handling MemberJoinedWorkspaceEvent: member={}, workspace={}, targets={}",
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
                        "workspaceKey", event.workspaceKey(),
                        "memberName", event.joinedMemberDisplayName(),
                        "role", event.role().name()));
    }

    /**
     * Target: Notify project admins
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberJoinedProject(MemberJoinedProjectEvent event) {
        Collection<WorkspaceMemberContact> targets =
                targetService.getProjectAdmins(event.workspaceKey(), event.projectKey());

        // Exclude if actor or joined member is an admin
        targets.removeIf(
                t -> t.memberId().equals(event.actorMemberId()) || t.memberId().equals(event.joinedMemberId()));

        log.info(
                "[NOTIFICATION] Handling MemberJoinedProjectEvent: member={}, project={}, targets={}",
                event.joinedMemberId(),
                event.projectKey(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forProjectMember(event.workspaceKey(), event.projectKey(), event.joinedMemberId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.MEMBER_JOINED_PROJECT,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        "projectKey", event.projectKey(),
                        "memberName", event.joinedMemberDisplayName(),
                        "role", event.role().name()));
    }

    /**
     * Target: Notify the member whose role changed
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWorkspaceRoleChanged(WorkspaceRoleChangedEvent event) {
        if (event.targetMemberId().equals(event.actorMemberId())) {
            return;
        }

        Collection<WorkspaceMemberContact> targets =
                targetService.getSpecificMemberTarget(event.workspaceKey(), event.targetMemberId());

        log.info(
                "[NOTIFICATION] Handling WorkspaceRoleChangedEvent: targetMember={}, newRole={}, targets={}",
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
                        "workspaceKey", event.workspaceKey(),
                        "memberName", event.targetDisplayName(),
                        "actorName", event.actorDisplayName(),
                        "oldRole", event.oldRole().name(),
                        "newRole", event.newRole().name()));
    }

    /**
     * Target: Notify the member whose role changed
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProjectRoleChanged(ProjectRoleChangedEvent event) {
        if (event.targetMemberId().equals(event.actorMemberId())) {
            return;
        }

        Collection<WorkspaceMemberContact> targets =
                targetService.getSpecificMemberTarget(event.workspaceKey(), event.targetMemberId());

        log.info(
                "[NOTIFICATION] Handling ProjectRoleChangedEvent: targetMember={}, project={}, newRole={}, targets={}",
                event.targetMemberId(),
                event.projectKey(),
                event.newRole(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference =
                EntityReference.forProjectMember(event.workspaceKey(), event.projectKey(), event.targetMemberId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.PROJECT_ROLE_CHANGED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        "projectKey", event.projectKey(),
                        "memberName", event.targetDisplayName(),
                        "actorName", event.actorDisplayName(),
                        "oldRole", event.oldRole().name(),
                        "newRole", event.newRole().name()));
    }
}
