package com.tissue.feature.notification.application.listener;

import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ACTOR_NAME;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.CONTENT;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ISSUE_KEY;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.WORKSPACE_KEY;

import com.tissue.feature.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.feature.comment.domain.event.IssueCommentUpdatedEvent;
import com.tissue.feature.notification.application.service.NotificationCommandService;
import com.tissue.feature.notification.application.service.NotificationTargetService;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberContactInfo;
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
public class CommentNotificationListener {

    private final NotificationCommandService commandService;
    private final NotificationTargetService targetService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueCommentAdded(IssueCommentAddedEvent event) {
        List<WorkspaceMemberContactInfo> mentionedMembers =
                targetService.getMembersByUsernames(event.workspaceKey(), new HashSet<>(event.mentionedUsernames()));

        removeActorFromTargets(mentionedMembers, event.actorMemberId());

        Set<WorkspaceMemberContactInfo> participants =
                targetService.getIssueParticipantsAndReviewers(event.workspaceKey(), event.issueKey());

        removeActorFromTargets(participants, event.actorMemberId());
        removeMentionedFromParticipants(mentionedMembers, participants);

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

        removeActorFromTargets(mentionedMembers, event.actorMemberId());

        Set<WorkspaceMemberContactInfo> participants =
                targetService.getIssueParticipantsAndReviewers(event.workspaceKey(), event.issueKey());

        removeActorFromTargets(participants, event.actorMemberId());
        removeMentionedFromParticipants(mentionedMembers, participants);

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

    private void removeMentionedFromParticipants(
            List<WorkspaceMemberContactInfo> mentionedMembers, Set<WorkspaceMemberContactInfo> participants) {

        Set<Long> mentionedMemberIds = mentionedMembers.stream()
                .map(WorkspaceMemberContactInfo::getMemberId)
                .collect(Collectors.toSet());
        participants.removeIf(p -> mentionedMemberIds.contains(p.getMemberId()));
    }

    private void removeActorFromTargets(Collection<WorkspaceMemberContactInfo> targets, Long memberId) {
        if (targets == null || memberId == null) {
            return;
        }
        targets.removeIf(target -> target.getMemberId().equals(memberId));
    }
}
