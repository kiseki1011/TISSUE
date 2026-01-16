package com.tissue.notification.application.listener;

import com.tissue.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.common.vo.EntityReference;
import com.tissue.issue.domain.event.IssueAssignedEvent;
import com.tissue.issue.domain.event.IssueCreatedEvent;
import com.tissue.issue.domain.event.IssueDeletedEvent;
import com.tissue.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.issue.domain.event.IssueReporterChangedEvent;
import com.tissue.issue.domain.event.IssueReviewRequestedEvent;
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
                "Handling IssueCreatedEvent: issue={} in project={}, target size={}",
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
                // args: {0}=projectKey, {1}=issueKey, {2}=actorDisplayName
                event.projectKey(),
                event.issueKey(),
                event.actorDisplayName());
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
        if (targets.isEmpty()) {
            return;
        }

        log.info("Handling IssueAssignedEvent: issue={}, assignee={}", event.issueKey(), event.assigneeMemberId());

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_ASSIGNED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                // args: {0}=issueKey, {1}=actorDisplayName
                event.issueKey(),
                event.actorDisplayName());
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
                "Handling IssueUnassignedEvent: issue={}, removedAssignee={}",
                event.issueKey(),
                event.removedAssigneeMemberId());

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_UNASSIGNED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                // args: {0}=issueKey, {1}=actorDisplayName
                event.issueKey(),
                event.actorDisplayName());
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
        if (targets.isEmpty()) {
            return;
        }

        String changedFields = String.join(", ", event.changes().keySet());

        log.info(
                "Handling IssueFieldsUpdatedEvent: issue={}, fields=[{}], targets={}",
                event.issueKey(),
                changedFields,
                targets.size());

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_UPDATED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                // args: {0}=issueKey, {1}=actorDisplayName, {2}=changedFields
                event.issueKey(),
                event.actorDisplayName(),
                changedFields);
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
        if (targets.isEmpty()) {
            return;
        }

        log.info(
                "Handling IssueTransitionedEvent: issue={}, {} -> {}, targets={}",
                event.issueKey(),
                event.oldStatusName(),
                event.newStatusName(),
                targets.size());

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_STATUS_CHANGED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                // args: {0}=issueKey, {1}=actorDisplayName, {2}=oldStatus, {3}=newStatus
                event.issueKey(),
                event.actorDisplayName(),
                event.oldStatusName(),
                event.newStatusName());
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
                // args: {0}=issueKey, {1}=actorDisplayName
                event.issueKey(),
                event.actorDisplayName());
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

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_REVIEWER_ADDED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                // args: {0}=issueKey, {1}=actorDisplayName
                event.issueKey(),
                event.actorDisplayName());
    }

    /**
     * Target: Notify assignee, reporter (exclude actor)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewSubmitted(com.tissue.issue.domain.event.IssueReviewSubmittedEvent event) {
        Collection<WorkspaceMemberContact> targets =
                targetService.getIssueAssigneeAndReporter(event.workspaceKey(), event.issueKey());

        targets.removeIf(t -> t.memberId().equals(event.actorMemberId()));
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
                // args: {0}=issueKey, {1}=actorDisplayName, {2}=status
                event.issueKey(),
                event.actorDisplayName(),
                event.status().name());
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
                // args: {0}=issueKey, {1}=actorDisplayName
                event.issueKey(),
                event.actorDisplayName());
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
                // args: {0}=issueKey, {1}=actorDisplayName
                event.issueKey(),
                event.actorDisplayName());
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

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_REVIEWER_REMOVED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                // args: {0}=issueKey, {1}=actorDisplayName
                event.issueKey(),
                event.actorDisplayName(),
                event.removedReviewerDisplayName());
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
                // args: {0}=issueKey, {1}=actorDisplayName
                event.issueKey(),
                event.actorDisplayName());
    }

    /**
     * Target: Notify project members (exclude actor)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSprintStarted(SprintStartedEvent event) {
        Collection<WorkspaceMemberContact> targets = targetService.getProjectMembersExcluding(
                event.workspaceKey(), event.projectKey(), event.actorMemberId());

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
                // args: {0}=workspaceKey, {1}=sprintName
                event.workspaceKey(),
                event.sprintTitle());
    }

    /**
     * Target: Notify project members (exclude actor)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSprintCompleted(SprintCompletedEvent event) {
        Collection<WorkspaceMemberContact> targets = targetService.getProjectMembersExcluding(
                event.workspaceKey(), event.projectKey(), event.actorMemberId());

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
                // args: {0}=workspaceKey, {1}=sprintName, {2}=startedAt, {3}=endedAt
                event.workspaceKey(),
                event.sprintTitle(),
                startedAt,
                endedAt);
    }

    /**
     * Target: Notify workspace admins
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberJoinedWorkspace(MemberJoinedWorkspaceEvent event) {
        // TODO: 해당 워크스페이스의 WorkspaceRole.ADMIN(OWNER 포함) 들로 타겟 변경(필요시 메서드 추가)
        Collection<WorkspaceMemberContact> targets =
                targetService.getAllWorkspaceMembersExcluding(event.workspaceKey(), event.joinedMemberId());

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
                // args: {0}=workspaceKey, {1}=memberName, {2}=role
                event.workspaceKey(),
                event.joinedMemberDisplayName(), // Assuming event has this
                event.role().name());
    }

    /**
     * Target: Notify project admins
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberJoinedProject(MemberJoinedProjectEvent event) {
        // TODO: 해당 프로젝트 ProjectRole.ADMIN 들로 타겟 변경(필요시 메서드 추가)
        Collection<WorkspaceMemberContact> targets = targetService.getProjectMembersExcluding(
                event.workspaceKey(), event.projectKey(), event.joinedMemberId());

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
                // args: {0}=projectKey, {1}=memberName, {2}=role
                event.projectKey(),
                event.joinedMemberDisplayName(),
                event.role().name());
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

        EntityReference reference = EntityReference.forWorkspaceMember(event.workspaceKey(), event.targetMemberId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.WORKSPACE_ROLE_CHANGED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                // args: {0}=workspaceKey, {1}=memberName, {2}=actorName, {3}=oldRole, {4}=newRole
                event.workspaceKey(),
                event.targetDisplayName(),
                event.actorDisplayName(),
                event.oldRole().name(),
                event.newRole().name());
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

        EntityReference reference =
                EntityReference.forProjectMember(event.workspaceKey(), event.projectKey(), event.targetMemberId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.PROJECT_ROLE_CHANGED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                // args: {0}=projectKey, {1}=memberName, {2}=actorName, {3}=oldRole, {4}=newRole
                event.projectKey(),
                event.targetDisplayName(),
                event.actorDisplayName(),
                event.oldRole().name(),
                event.newRole().name());
    }
}
