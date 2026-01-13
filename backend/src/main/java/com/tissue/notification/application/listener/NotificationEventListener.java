package com.tissue.notification.application.listener;

import com.tissue.issue.domain.event.IssueAssignedEvent;
import com.tissue.issue.domain.event.IssueCreatedEvent;
import com.tissue.issue.domain.event.IssueDeletedEvent;
import com.tissue.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.issue.domain.event.IssueReporterChangedEvent;
import com.tissue.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.issue.domain.event.IssueTransitionedEvent;
import com.tissue.issue.domain.event.IssueUnassignedEvent;
import com.tissue.notification.application.service.NotificationCommandService;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.vo.EntityReference;
import com.tissue.workspace.application.port.out.WorkspaceMemberContact;
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
    private final com.tissue.notification.application.service.command.NotificationTargetService targetService;

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
}
