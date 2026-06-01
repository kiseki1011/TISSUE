package com.tissue.feature.notification.application.service;

import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ACTOR_NAME;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ISSUE_KEY;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.NEW_STATE;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.OLD_STATE;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.REMOVED_REVIEWER_NAME;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.STATUS;

import com.tissue.feature.issue.domain.event.IssueAssignedEvent;
import com.tissue.feature.issue.domain.event.IssueDeletedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewRequestedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewerRemovedEvent;
import com.tissue.feature.issue.domain.event.IssueTransitionedBySystemEvent;
import com.tissue.feature.issue.domain.event.IssueTransitionedEvent;
import com.tissue.feature.issue.domain.event.IssueUnassignedEvent;
import com.tissue.feature.member.application.port.repository.MemberContactInfo;
import com.tissue.feature.notification.application.port.usecase.IssueNotificationUseCase;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.shared.vo.EntityReference;
import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueNotificationService implements IssueNotificationUseCase {

    private final NotificationCommandService commandService;
    private final NotificationTargetService targetService;

    @Override
    public void handleIssueAssigned(IssueAssignedEvent event) {
        Collection<MemberContactInfo> targets = targetService.getIssueAssignee(event.issueKey());

        removeActorFromTargets(targets, event.actorMemberId());

        log.info(
                "Handling IssueAssignedEvent: issue={}, assignee={}, targets={}",
                event.issueKey(),
                event.assigneeMemberId(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forIssue(event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_ASSIGNED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(ISSUE_KEY, event.issueKey(), ACTOR_NAME, event.actorDisplayName()));
    }

    @Override
    public void handleIssueUnassigned(IssueUnassignedEvent event) {
        if (event.removedAssigneeMemberId().equals(event.actorMemberId())) {
            return;
        }

        Collection<MemberContactInfo> targets = targetService.getSpecificMemberTarget(event.removedAssigneeMemberId());

        log.info(
                "Handling IssueUnassignedEvent: issue={}, removedAssignee={}, targets={}",
                event.issueKey(),
                event.removedAssigneeMemberId(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forIssue(event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_UNASSIGNED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(ISSUE_KEY, event.issueKey(), ACTOR_NAME, event.actorDisplayName()));
    }

    @Override
    public void handleIssueTransitioned(IssueTransitionedEvent event) {
        Collection<MemberContactInfo> targets = targetService.getIssueParticipantsAndReviewers(event.issueKey());

        removeActorFromTargets(targets, event.actorMemberId());

        log.info(
                "Handling IssueTransitionedEvent: issue={}, {} -> {}, targets={}",
                event.issueKey(),
                event.oldStateName(),
                event.newStateName(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forIssue(event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_STATUS_CHANGED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        ISSUE_KEY,
                        event.issueKey(),
                        ACTOR_NAME,
                        event.actorDisplayName(),
                        OLD_STATE,
                        event.oldStateName(),
                        NEW_STATE,
                        event.newStateName()));
    }

    @Override
    public void handleTransitionedBySystem(IssueTransitionedBySystemEvent event) {
        Collection<MemberContactInfo> targets = targetService.getIssueParticipantsAndReviewers(event.issueKey());

        log.info(
                "Handling IssueTransitionedBySystemEvent: issue={}, {} -> {}, targets={}",
                event.issueKey(),
                event.oldStateName(),
                event.newStateName(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forIssue(event.projectKey(), event.issueKey());

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
                        ISSUE_KEY,
                        event.issueKey(),
                        ACTOR_NAME,
                        actorName,
                        OLD_STATE,
                        event.oldStateName(),
                        NEW_STATE,
                        event.newStateName()));
    }

    @Override
    public void handleIssueReviewerAdded(IssueReviewerAddedEvent event) {
        if (event.reviewerMemberId().equals(event.actorMemberId())) {
            return;
        }

        Collection<MemberContactInfo> targets = targetService.getSpecificMemberTarget(event.reviewerMemberId());

        log.info(
                "Handling IssueReviewerAddedEvent: issue={}, reviewer={}, targets={}",
                event.issueKey(),
                event.reviewerMemberId(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forIssue(event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_REVIEWER_ADDED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(ISSUE_KEY, event.issueKey(), ACTOR_NAME, event.actorDisplayName()));
    }

    @Override
    public void handleIssueReviewSubmitted(IssueReviewSubmittedEvent event) {
        Collection<MemberContactInfo> targets = targetService.getIssueAssigneeAndReporter(event.issueKey());

        removeActorFromTargets(targets, event.actorMemberId());

        log.info(
                "Handling IssueReviewSubmittedEvent: issue={}, status={}, targets={}",
                event.issueKey(),
                event.reviewStatus(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forIssue(event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_REVIEW_SUBMITTED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        ISSUE_KEY,
                        event.issueKey(),
                        ACTOR_NAME,
                        event.actorDisplayName(),
                        STATUS,
                        event.reviewStatus().name()));
    }

    @Override
    public void handleIssueDeleted(IssueDeletedEvent event) {
        Collection<MemberContactInfo> targets = targetService.getIssueParticipantsAndReviewers(event.issueKey());

        removeActorFromTargets(targets, event.actorMemberId());

        log.info("Handling IssueDeletedEvent: issue={}, targets={}", event.issueKey(), targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forIssue(event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_DELETED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(ISSUE_KEY, event.issueKey(), ACTOR_NAME, event.actorDisplayName()));
    }

    @Override
    public void handleIssueReviewerRemoved(IssueReviewerRemovedEvent event) {
        if (event.removedReviewerMemberId().equals(event.actorMemberId())) {
            return;
        }

        Collection<MemberContactInfo> targets = targetService.getSpecificMemberTarget(event.removedReviewerMemberId());

        log.info(
                "Handling IssueReviewerRemovedEvent: issue={}, removedReviewer={}, targets={}",
                event.issueKey(),
                event.removedReviewerMemberId(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forIssue(event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_REVIEWER_REMOVED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        ISSUE_KEY,
                        event.issueKey(),
                        ACTOR_NAME,
                        event.actorDisplayName(),
                        REMOVED_REVIEWER_NAME,
                        event.removedReviewerDisplayName()));
    }

    @Override
    public void handleIssueReviewRequested(IssueReviewRequestedEvent event) {
        Collection<MemberContactInfo> targets;

        if (event.reviewerMemberIds() != null && !event.reviewerMemberIds().isEmpty()) {
            targets = targetService.getSpecificMembersTargets(event.reviewerMemberIds());
        } else {
            targets = targetService.getIssueReviewers(event.issueKey());
        }

        removeActorFromTargets(targets, event.actorMemberId());

        log.info("Handling IssueReviewRequestedEvent: issue={}, targets={}", event.issueKey(), targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forIssue(event.projectKey(), event.issueKey());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_REVIEW_REQUESTED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(ISSUE_KEY, event.issueKey(), ACTOR_NAME, event.actorDisplayName()));
    }

    private void removeActorFromTargets(Collection<MemberContactInfo> targets, Long memberId) {
        if (targets == null || memberId == null) {
            return;
        }
        targets.removeIf(target -> target.getMemberId().equals(memberId));
    }
}
